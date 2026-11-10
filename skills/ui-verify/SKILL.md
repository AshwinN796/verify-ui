---
name: ui-verify
description: Render a Jetpack Compose composable's @Previews in every state that matters (plus 2x font, RTL and dark), check the renders for layout and accessibility bugs, and fix them until the checks pass. Use when the user runs /ui-verify <ComposableName>, or asks to verify, check or screenshot-test how a composable looks.
---

# /ui-verify <ComposableName>

You cannot see the UI you write. This loop gives you eyes: previews describe the states,
the verify-ui Gradle plugin renders them and reports what is wrong, you look at the images,
fix the code and render again.

## 0. Ground rules

- Fix the composable, never the measurement. Do not edit generated files under
  `build/generated/verifyui`, do not change plugin settings to make a check pass.
- Do not delete or weaken a preview to make it pass (no shortening long-text data,
  no removing a state).
- No placeholder labels like "button" or "icon". A label says what the control does
  ("Remove Masala Dosa").
- Shell: on Windows use `.\gradlew.bat` (PowerShell) or `./gradlew` (Git Bash).

## 1. Setup (first run in a project only)

Find the module that contains the composable (the one with `com.android.application` or
`com.android.library`). If its `plugins { }` block has no `io.github.ashwinn796.verify-ui`, add it:

```kotlin
plugins {
    // ...existing plugins
    id("io.github.ashwinn796.verify-ui") version "0.1.0"
}
```

If the project uses a version catalog, add it there instead
(`verifyui = { id = "io.github.ashwinn796.verify-ui", version = "0.1.0" }` and `alias(libs.plugins.verifyui)`).
Make sure `settings.gradle.kts` `pluginManagement.repositories` contains `gradlePluginPortal()`.

Requirements to check, and tell the user if one is missing:
- The module uses a Compose BOM: `implementation(platform("androidx.compose:compose-bom:..."))`.
- `android.namespace` is set (the plugin scans previews under it).

Nothing else is needed: the plugin adds Robolectric, Roborazzi and the test code itself.

## 2. Read the composable

- Find `fun <ComposableName>(` and read the whole file.
- Note the parameters: `String` (needs long text), `List` (needs empty and many),
  `Boolean` (needs both values), UI-state types (sealed class: one state per subtype),
  lambdas (pass `{}`).
- If it needs a ViewModel, Hilt, navigation or a repository, it cannot be previewed.
  Stop and offer to split it into a stateless composable that takes plain data.

## 3. Write the previews (they are the spec)

In the composable's file, add or update one `@Preview` per state, named
`<ComposableName><State>Preview`:

| Preview name                         | Content                                        |
|--------------------------------------|------------------------------------------------|
| `<Name>Preview`                      | realistic typical data (this is "default")     |
| `<Name>LongTextPreview`              | every String at a realistic maximum            |
| `<Name>EmptyPreview`                 | empty lists, if any List parameter             |
| `<Name>ManyItemsPreview`             | 20+ items, if any List parameter               |
| `<Name>LoadingPreview` / `ErrorPreview` / one per UI-state subtype | if the composable has a UI state |

Rules:
- Wrap each in the app theme and a `Surface`, the way the app's Scaffold hosts it:
  `AppTheme(dynamicColor = false) { Surface { <Name>(...) } }`. Without the Surface, text with no
  explicit color renders black in dark mode and the contrast check fails for the wrong reason.
  Pass `dynamicColor = false` if the theme supports it, so renders are deterministic.
- `private` previews are fine. Use `@Preview(showBackground = true)`.
- Do NOT write font-scale, RTL or dark previews: the plugin renders the default preview
  in those variants automatically.
- Long text must be realistic for the product (a long dish name, a long full name, a
  German-length label), not lorem ipsum.

## 4. Render and check

```
./gradlew :<module>:verifyUi -Pcomposable=<ComposableName>
```

The task prints a summary and writes, under `<module>/build/verify-ui/<ComposableName>/`:
- `<state>.png` and `<state>.semantics.json` for every preview,
- `default__font_2x`, `default__rtl`, `default__dark` variants of the default preview,
- `report.json`: `status`, and `findings` with `rule`, `states`, `element` (how to find it,
  even if unlabeled), `message` and `fix_hint`,
- `<state>.error.txt` if a preview crashed (reported as `render_failed`).

## 5. Look, then fix

1. Read `report.json`.
2. Open every PNG with the Read tool and look at it. The checker does not catch everything:
   overlaps, awkward alignment, inconsistent spacing, icons that should flip in RTL, a state
   that looks broken. Note anything wrong that the report missed.
   Also check controls the semantics tree cannot see: a `Switch`/`Checkbox` with
   `onCheckedChange = null` inside a `.clickable` row announces no state to TalkBack; the row
   should use `Modifier.toggleable(value, role = Role.Switch, onValueChange)`.
3. Fix the composable source. Prefer theme colors and typography over hard-coded values,
   `heightIn(min = )` over `height()`, `weight()` on flexible text, start/end over left/right,
   `Modifier.toggleable` / `selectable` with a `Role` for rows that act as controls,
   `IconButton` (48dp) for icon actions.
   Keep each fix minimal and local to the finding. Do not change structure that no finding
   asks for (e.g. `Column` -> `LazyColumn`, new weights on containers, new parameters): it
   changes behavior for every caller and can break the composable in other parents. If you
   think a structural change is needed, propose it in the final report instead of making it.
4. Run step 4 again. Stop when the status is `pass` and the images look right, or after
   5 rounds. If a fix would change the intended design (a fixed-size spec), stop and explain
   the trade-off instead of forcing it.

## 6. Report to the user

- Status: pass / fail, after N rounds.
- A table: finding → states → what you changed (file:line).
- Problems you saw in the images that the checker did not flag, and whether you fixed them.
- Warnings left as is, with the reason.
- Paths of the final PNGs so the user can open them.
