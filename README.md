# verify-ui

AI agents can write a Compose screen in seconds. They just can't see what they built.

verify-ui gives the agent eyes. It renders your `@Preview`s in every state that matters
(as written, 2x font, RTL, dark), checks the renders for layout and accessibility bugs,
and hands the agent a report it can act on: what is broken, where, in which state, and how to fix it.

```
OrderSummaryCard: FAIL (example output from the sample app)
  [error] collapsed      long_text          text '₹380' has zero visible size; it is squeezed out of the layout.
  [error] missing_label  default, ...       unlabeled clickable (24x24dp) right of '₹120' has no text or contentDescription.
  [error] text_clipped   default__font_2x   Text '₹160' needs 63x42dp but got 63x24dp.
  [error] touch_target   default, ...       clickable 'Pay ₹160' (347x36dp) is smaller than 48x48dp.
```

## Use it with Claude Code

1. Install the skill (copy `skills/ui-verify` into your project's `.claude/skills/`).
2. In Claude Code: `/ui-verify OrderSummaryCard`

On the first run the skill adds one line to your module's `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.ashwinn796.verify-ui") version "0.1.0"
}
```

Then it writes previews for the composable's states, renders them, reads the report,
looks at the images, fixes the code and repeats until it passes.

## Use it without an agent

```
./gradlew :app:verifyUi -Pcomposable=OrderSummaryCard
```

Output: `app/build/verify-ui/OrderSummaryCard/` with a PNG and semantics dump per state,
and `report.json`.

## What it checks

| Rule               | Catches                                                        |
|--------------------|----------------------------------------------------------------|
| `touch_target`     | Clickable elements smaller than 48x48dp                        |
| `missing_label`    | Clickables with no text or contentDescription                  |
| `collapsed`        | Elements squeezed to zero size (e.g. a missing `weight`)       |
| `text_clipped`     | Text cut off by a fixed height/width, usually at large font    |
| `text_truncated`   | Ellipsized text (warning)                                      |
| `toggle_semantics` | Rows that toggle a switch but expose no role or state          |
| `rtl_not_mirrored` | Layouts that stay left-aligned in RTL                          |
| `low_contrast`     | Text below WCAG AA contrast, measured from the rendered pixels |
| `render_failed`    | Previews that crash (e.g. need a ViewModel)                    |

A passing report means the screen survives these states. It does not mean the design is right:
that is still a human review.

## Requirements

- Android Gradle Plugin 9.x (8.x untested), Compose with a Compose BOM, `android.namespace` set.
- Renders run on the JVM with Robolectric (SDK 36), Roborazzi and ComposablePreviewScanner,
  added to your test classpath by the plugin. No emulator, no Android Studio needed.

## Repository layout

```
verify-ui-gradle-plugin/   Gradle plugin: renderer runtime, checker, verifyUi task
skills/ui-verify/          The Claude Code skill
app/                       Sample app with planted UI bugs, used to test the tool
tooling/eval/              Answer key for the planted bugs
```

## Status

Early prototype. Not yet published: the sample uses the plugin via `includeBuild`.
