# verify-ui Gradle plugin

`io.github.ashwinn796.verify-ui` renders the `@Preview`s of a Jetpack Compose composable in the states
that usually break layouts (as written, 2x font scale, RTL, dark), then checks the renders for layout and
accessibility bugs. It writes PNGs, a semantics dump per state and a `report.json`, and prints a summary.

It runs on the JVM (Robolectric + Roborazzi). No emulator or Android Studio is needed.
It works from the command line and in CI, with or without an AI agent.

## Setup

In the `build.gradle.kts` of an Android application or library module that uses Compose:

```kotlin
plugins {
    id("com.android.application") // or com.android.library
    id("io.github.ashwinn796.verify-ui") version "0.2.1"
}
```

Requirements:

- Android Gradle Plugin 9.x (8.x untested).
- Compose with a Compose BOM (the plugin adds `ui-test-junit4` and `ui-test-manifest` without versions).
- `android.namespace` set, or `verifyUi.scanPackages` configured.
- Applying the plugin to a module without the Android application/library plugin fails with a clear error.

The plugin adds these to the module's test classpath: Robolectric, Roborazzi (`roborazzi`, `roborazzi-compose`),
ComposablePreviewScanner and `androidx.test.ext:junit`. It also enables `unitTests.includeAndroidResources`.

## Usage

Write one or more `@Preview` functions for a composable, then:

```
./gradlew :app:verifyUi -Pcomposable=OrderSummaryCard
```

(Use `-Pcomposable=...` rather than `-PverifyUi.composable=...` on PowerShell, which splits at the dot.
The dotted form still works elsewhere.)

Every `@Preview` that belongs to the composable is rendered. Previews are matched by name:
`OrderSummaryCardPreview`, `OrderSummaryCardLongTextPreview`, and so on. The default preview is also rendered at
2x font scale, in RTL and in dark theme. If nothing matches, the task says which preview names to add.

Example output:

```
OrderSummaryCard: FAIL
  [error] collapsed      long_text          text '₹380' has zero visible size; it is squeezed out of the layout.
  [error] touch_target   default, ...       clickable 'Pay ₹160' (347x36dp) is smaller than 48x48dp.
  report: app/build/verify-ui/OrderSummaryCard/report.json
```

Output goes to `<module>/build/verify-ui/<Composable>/`: one PNG and one semantics dump per state, plus `report.json`.
The task never reports "up to date"; it re-renders on every run.

## Checks

| Rule               | Catches                                                        |
|--------------------|----------------------------------------------------------------|
| `touch_target`     | Clickable elements smaller than 48x48dp                        |
| `missing_label`    | Clickables with no text or contentDescription                  |
| `collapsed`        | Elements squeezed to zero size (e.g. a missing `weight`)       |
| `text_clipped`     | Text cut off by a fixed height/width, usually at large font    |
| `text_clipped_silent` | Text cut by `maxLines` with no ellipsis, so the end is lost |
| `text_truncated`   | Ellipsized text (warning)                                      |
| `toggle_semantics` | Rows that toggle a switch but expose no role or state          |
| `rtl_not_mirrored` | Layouts that stay left-aligned in RTL                          |
| `low_contrast`     | Text below WCAG AA contrast, measured from the rendered pixels |
| `render_failed`    | Previews that crash (e.g. need a ViewModel)                    |

A passing report means the screen survives these states. It does not replace design review.

## Configuration

All settings are optional.

```kotlin
verifyUi {
    variant = "prodDebug"                    // build variant whose unit tests render the previews
    scanPackages = listOf("com.example.ui")  // packages scanned for @Preview (default: android.namespace)
    robolectricSdk = 36                      // Android SDK Robolectric renders with
    robolectricVersion = "4.16.1"
    roborazziVersion = "1.58.0"
    previewScannerVersion = "0.9.1"
    androidxTestJunitVersion = "1.3.0"
}
```

| Property                   | Default                                              |
|----------------------------|------------------------------------------------------|
| `variant`                  | `debug`; with flavors, the first `<flavor>Debug`     |
| `scanPackages`             | `android.namespace`                                  |
| `robolectricSdk`           | `36`                                                 |
| `robolectricVersion`       | `4.16.1`                                             |
| `roborazziVersion`         | `1.58.0`                                             |
| `previewScannerVersion`    | `0.9.1`                                              |
| `androidxTestJunitVersion` | `1.3.0`                                              |

Command-line properties:

| Property                         | Meaning                                                           |
|----------------------------------|-------------------------------------------------------------------|
| `-Pcomposable=<Name>`            | Composable to render and check (required in practice)             |
| `-PverifyUiVariant=<variant>`    | Overrides `verifyUi.variant` for one run, e.g. `prodDebug`        |

## How it works

1. `verifyUiGenerateRuntime` copies a small renderer and one parameterized test into
   `build/generated/verifyui/test` and registers it as a unit-test source directory.
2. The module's debug unit-test task runs only that test, and only when `verifyUi` is requested.
   In every other build the generated test is excluded, so `./gradlew test` is unaffected.
3. `verifyUi` runs the checker over the renders and prints the summary.

## Using it with an AI agent

The repository also ships a Claude Code skill (`/ui-verify`) that drives this task in a loop:
render, read the report, fix the code, repeat. See the [main README](../README.md).
The plugin does not depend on it.

## License

MIT