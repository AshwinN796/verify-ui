# verify-ui

Gives AI agents eyes for Jetpack Compose: render a composable's `@Preview`s in every state
that matters, check the renders for layout and accessibility bugs, hand the agent a report,
loop until it passes. Previews are the spec; screenshots are the eyes. Product vision doc:
https://claude.ai/artifact/2TCHpwXtksxBisU8xLz23Q ("composeye").

Current status, plan and open issues: `docs/progress/README.md` (read it first, update it at the
end of every session and add a dated log `docs/progress/YYYY-MM-DD.md`).

## Layout

| Path | What |
|------|------|
| `verify-ui-gradle-plugin/` | Gradle plugin `io.github.ashwinn796.verify-ui`: `verifyUi` task, checker, runtime templates |
| `verify-ui-gradle-plugin/src/main/resources/verifyui-runtime/*.kt.txt` | Kotlin source templates copied into the user's unit-test source set (`Renderer`, `SemanticsDump`, `VerifyUiTest`). Not compiled in this repo; edit as text |
| `verify-ui-gradle-plugin/src/main/kotlin/verifyui/gradle/check/` | Rule-based checker (Kotlin, no deps) that reads the semantics dumps and writes `report.json` |
| `plugins/verify-ui/skills/ui-verify/SKILL.md` | The `/ui-verify` skill (canonical). `.claude/skills/ui-verify/` is the local copy: keep both in sync |
| `app/` | Sample app ("testbed") with planted UI bugs, used to test the tool. Screens in `app/src/main/java/com/verifyui/ashwinn796/testbed/` |
| `tooling/eval/` | Answer key (`planted-bugs.md`) and agent run transcripts. Update when a testbed screen or rule changes |

## Commands

```
./gradlew :app:verifyUi -Pcomposable=OrderSummaryCard   # render + check one composable
./gradlew :app:verifyUi -Pcomposable=X -PverifyUiVariant=prodDebug   # flavored modules
```

Output: `app/build/verify-ui/<Composable>/` with `<state>.png`, `<state>.semantics.json`, `report.json`.
The sample consumes the plugin through `includeBuild`, so plugin edits apply on the next run.

## How rendering works

`Renderer.kt.txt` scans `@Preview`s with ComposablePreviewScanner, groups them per composable,
and renders each preview as-is. The preview named `default` (or the first one) is also rendered
under extra `Variant`s: `font_2x`, `rtl`, `dark`. Variants are data (`Variant(suffix, fontScale, rtl, dark)`),
so a new variant means a new entry in `Variant` plus the loop in `RenderCases.load()`.
The checker works on geometry (needed vs given size), not `hasVisualOverflow` (unreliable on Robolectric).

## Rules for working here

- Fix the composable, never the measurement. Do not weaken a checker rule or a preview to make a test screen pass.
- Testbed screens: keep planted bugs intentional and listed in `tooling/eval/planted-bugs.md`. No hint comments in test screens.
- `/ui-verify` fixes must be minimal and local (see the skill). Do not change structure no finding asks for.
- Use `-Pcomposable=`, not `-PverifyUi.composable=` (PowerShell splits at the dot).
- Do not edit generated files under `build/generated/verifyui`.
- Gradle plugin version in samples is `0.1.0`; not yet on the Plugin Portal.
- Commit only when asked.

## Roadmap pointers

Next: interaction-state previews (checkbox, radio, switch, toggle) generated from the composable's
controls, not only 2x/RTL/dark; then real-app validation (Bein), Plugin Portal publish,
`/ui-match`, CI PR evidence. Details in `docs/progress/README.md`.