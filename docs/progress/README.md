# verify-ui progress

Living status of the project. Update this file at the end of every working session,
and add a dated log next to it (`YYYY-MM-DD.md`) with what happened in that session.

Last updated: 2026-11-18

## Goal

Give AI coding agents eyes for Jetpack Compose: render `@Preview`s in every state that
matters (as written, 2x font, RTL, dark), check the renders for layout and accessibility
bugs, and let the agent fix them in a loop. Previews are the spec; screenshots are the eyes.

## Components

| Component | Path | Status |
|-----------|------|--------|
| Gradle plugin `io.github.ashwinn796.verify-ui` | `verify-ui-gradle-plugin/` | Works on the sample (AGP 9). Real-app (AGP 8 + kapt, flavors) in progress |
| Renderer runtime (generated test, variants, semantics dump) | `verify-ui-gradle-plugin/src/main/resources/verifyui-runtime/` | Works |
| Checker (9 rules, Kotlin, no deps) | `verify-ui-gradle-plugin/src/main/kotlin/verifyui/gradle/check/` | Works; 0 false positives on sample |
| Claude Code skill `/ui-verify` | `plugins/verify-ui/skills/ui-verify/` (local copy in `.claude/skills/`) | Works in agent tests |
| Claude Code marketplace | `.claude-plugin/marketplace.json` | Written, not yet validated with `claude plugin validate .` |
| Sample app with planted bugs | `app/` | 3 screens, 11 bugs; 2 new real-world screens added (not yet verified) |
| Eval answer key and scoreboard | `tooling/eval/` | Up to date through run 4 |

## Scoreboard (sample app)

| Measure | Result |
|---------|--------|
| Planted bugs found by checker | 10 / 11 (SR-3 not visible in semantics; covered by skill's visual checklist) |
| False positives | 0 (after runs 1 and 4 fixes) |
| Agent fixes, clean run, no hints | 8 / 8 planted bugs on SettingsRow + OrderSummaryCard, plus 1 unplanted bug |

Details: `tooling/eval/planted-bugs.md`.

## Plan and status

| # | Step | Status |
|---|------|--------|
| 1 | Prove rendering + semantics dump (spike) | Done |
| 2 | Rule-based checker, scored against planted bugs | Done (10/11) |
| 3 | Package as Gradle plugin + `@Preview`-driven test (no per-composable test files) | Done |
| 4 | `/ui-verify` skill; clean agent test | Done (run 4) |
| 5 | Packaging: MIT license, Claude Code marketplace, Plugin Portal publishing config | Done (not yet published) |
| 6 | Test on a real app (Bein: AGP 8, kapt, 6 flavors, multi-module) | **In progress** |
| 6b | Interaction-state previews (`docs/specs/interaction-previews.md`): skill step done, `SemanticsDump` `selected`/`enabled`, checker rules and action-driven renderer next | **In progress** |
| 7 | Publish Gradle plugin to Plugin Portal (review takes days) | Not started |
| 8 | `/ui-match <Composable> design.png` (compare against a reference screenshot) | Not started |
| 9 | CI action: before/after screenshots on UI PRs; baseline protection | Not started |

## Open issues

| Issue | Found | Notes |
|-------|-------|-------|
| AGP 9: generated sources now registered on both `sources.java` and `sources.kotlin` (commit d333a8a). May compile the runtime twice ("Redeclaration") | 2026-11-17 | Needs a sample-app run on AGP 9 after d333a8a. If it breaks, register `kotlin` only when the built-in Kotlin compiler is used, else `java` |
| Real-app unit-test manifest merge fails when a library SDK has higher minSdk (T5Face3D) | 2026-11-10 | Triggered by the plugin enabling `includeAndroidResources`. Workaround: `src/debug/AndroidManifest.xml` with `tools:overrideLibrary`. README troubleshooting section still to write |
| Compose test artifacts are added without a version (rely on a Compose BOM) | 2026-11-10 | Projects without a BOM on `implementation` will fail to resolve. Plugin should detect the Compose UI version in use |
| SR-3 (Switch with null callback in clickable row) invisible to semantics | 2026-11-10 | Covered by skill checklist only |
| `UserRoleCard` / `ComingSoonPill` added to sample, not yet rendered or added to the answer key | 2026-11-17 | See 2026-11-17 log for bugs worth recording |

## Decisions

| Date | Decision | Why |
|------|----------|-----|
| 2026-11-10 | Build on existing renderers (Roborazzi, Robolectric, ComposablePreviewScanner) instead of writing one | Rendering is a commodity; the value is the checks, the loop and the report |
| 2026-11-10 | Previews are the spec; no per-composable test files | Developers already write previews; one plugin line is the whole setup |
| 2026-11-10 | Checker in Kotlin inside the plugin, not Python | No extra runtime for users |
| 2026-11-10 | Geometry (needed vs given size) instead of `hasVisualOverflow` | `hasVisualOverflow` is unreliable under Robolectric |
| 2026-11-10 | `-Pcomposable=` instead of `-PverifyUi.composable=` | PowerShell splits `-Pa.b=c` at the dot |
| 2026-11-10 | Skill requires minimal, local fixes | Agent turned a Column into a LazyColumn without being asked (run 4) |
| 2026-11-18 | Interaction states: each control alone + baseline + all-on + disabled pairs (N+2 renders, not 2^N); interaction states get `dark` only (plus `font_2x` if the state adds text); controls addressed by testTag, then label, then order | Linear cost; bugs sit where states meet; flaky addressing is a finding. See spec |
| 2026-11-10 | MIT license; namespace `io.github.ashwinn796` | Plugin Portal verifies io.github namespaces via GitHub |
