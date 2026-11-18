# Interaction-based previews

Status: in progress. Written 2026-11-17. Open questions decided 2026-11-18. Build step 1 (skill) done; steps 2-4 not started.

## Problem

verify-ui renders each composable's `default` preview under three fixed variants:
`font_2x`, `rtl` and `dark` (`Variant` in `Renderer.kt.txt`). It does not render the states
that controls produce: a checkbox checked or unchecked, a radio selected, a switch on or off,
a selectable card selected, a control disabled. Bugs that only appear in those states are missed.
Example: `UserRoleCard` selected state shows an empty green dot with no label, and nothing renders it.

## Goal

Generate previews from the composable's controls, so the agent and the checker cover interaction
states without a hand-written preview per state.

## Detection

The semantics dump already records per node `toggleState`, `role`, `clickable` and `testTag`.
A first pass finds controls from it:

- Checkbox / Switch: `toggleState` present.
- Radio / selectable row: `role` of `RadioButton` or `Tab`, or a `selected` property (to be added to `SemanticsDump`).
- Plain clickable: `clickable` true.

Controls that expose no semantics (for example a bare `Box` with `.clickable`) are invisible to
this step. That gap is itself a finding (`toggle_semantics`).

## Who writes the previews

The plugin renders only the previews that exist; it does not invent states.

- With `/ui-verify`, the agent writes the state previews (build step 1). The developer writes nothing.
- With the Gradle plugin alone, the developer writes a preview per state. One preview gives one
  state, plus the `font_2x`, `rtl` and `dark` variants of `default`.
- Param-driven previews cannot reach state held inside the composable (`remember`) and cannot
  invent parameter values.
- Action-driven rendering (build step 4) removes the need for per-state previews: the plugin
  clicks each detected control itself and also covers internal state. Not started.

User-facing version of this is in the root `README.md`, section "Who writes the previews".

## Two approaches

### 1. Param-driven (start here)

State lives in parameters (stateless composable: `isSelected`, `termsChecked`, `enabled`).
The skill writes previews per boolean parameter value, using `@PreviewParameterProvider` or one
`@Preview` per state, named `<Name><State>Preview`, where State comes from the parameter name (for example `UserRoleCardSelectedPreview`).

- Pros: no checker or renderer change; deterministic; fast.
- Cons: needs state hoisted; does not cover state held inside the composable.

Skill change: in `ui-verify` step 3, `Boolean` parameters and sealed UI-state types already ask for one
preview per value. Extend it to name the control-driven states explicitly and to pair them
(selected + checked, disabled + checked).

### 2. Action-driven (later)

Render once, then for each detected control run `performClick()` (or `performClick` on its `testTag`),
let the composition settle, and capture a new image and semantics dump named
`<state>__after_<control>`.

- Pros: covers state held inside the composable; proves the control actually responds.
- Cons: slower; flaky with animations; needs a stable way to address each control.

Implementation sketch: `RenderCase` gets an optional `actions: List<Action>`; `Renderer` runs them
through `ComposeContentTestRule` before capture; output file name gets the action suffix.

## New checks this enables

| Rule | Catches |
|------|---------|
| `state_not_changed` | Control clicked, but toggle/selected state and pixels unchanged (action-driven) |
| `state_not_announced` | Selected or checked look differs but semantics expose no selected/toggle state (for example the empty selected dot) |
| Existing rules per state | `touch_target`, `low_contrast`, `text_clipped` run on every interaction state, not only `default` |

## Output

Same folder layout as today, `build/verify-ui/<Composable>/`:

```
default.png                    # as written
selected.png                   # param-driven preview
default__after_terms.png       # action-driven: after clicking the "terms" checkbox
default__font_2x.png ...       # existing variants, unchanged
```

`report.json` findings list the state name, so the agent knows which state to fix.

## Test case

`UserRoleCard` (testbed): selected, coming-soon, terms checked, long description. Bugs to confirm
by render are listed in `docs/progress/2026-11-17.md`. Record confirmed ones in `tooling/eval/planted-bugs.md`.

## Decisions

### 0. States come from the parameter list

States are derived from the composable's parameters by type, never from names the tool recognises
(`isComingSoon` is a testbed example, not a rule). Boolean: opposite of the default. Enum/sealed:
one per value. Nullable: null and non-null. Lambdas: not a state. Preview names come from the
parameter (`isSelected` -> `Selected`, `termsChecked` -> `TermsChecked`). Data parameters
(`title`, `description`) are covered by the long-text and empty/many previews. A later plugin-side
generator can read the same parameter list (KSP or reflection on the `@Preview` target).


Open questions from the first draft, decided 2026-11-18.

### 1. Which state combinations to render

Each control alone, plus baseline, plus all-on, plus known-interacting pairs. For N independent
boolean controls that is N + 2 renders (linear), not 2^N.

- Baseline: all off (this is `default`).
- Each control alone: `selected`, `terms_checked`, `coming_soon`, ...
- All-on: catches overlap between states (a selected dot colliding with a coming-soon badge).
- Pairs only where one parameter changes how the others render. `enabled = false` (and
  `isComingSoon`-style dimming) is the main case: render `disabled + selected`, `disabled + checked`.
- Enum / sealed state types: one render per value; do not cross them with booleans except disabled pairs.
- Guard rail: more than about 6 control states means baseline, each alone and all-on only. The
  skill states in its report that pairs were skipped, so the gap is visible.

Why: bugs sit where two states meet, but most of the 2^N combinations add no information and
every render multiplies by the checker rules and variants.

### 2. Which variants apply to interaction states

`default` stays under all three variants (`font_2x`, `rtl`, `dark`). Interaction states render
as-written plus `dark` (state colors are where contrast bugs live). `font_2x` and `rtl` are not
repeated per state. Exception: a state that adds or swaps text (for example an error message
or a "coming soon" badge) also gets `font_2x`, because that is where `text_clipped` shows up.
Applies from build step 3 (renderer loop); until then interaction previews render as-written only.

Why: the cost of variants multiplies per state; layout and RTL mirroring are mostly decided by
the default layout, while color and added text are decided by state.

### 3. Addressing a control without a `testTag`

Resolution order for action-driven cases: `testTag`, then the node's text or content description
(exact match), then semantics node order among nodes of the same role. A control resolved by
anything but `testTag` is reported as `unstable_control_address` (warning) with the fallback used,
and the fix hint is "add a testTag or a content description". A control with no semantics at all
cannot be found and stays the `toggle_semantics` finding from Detection.

Why: a flaky address makes `state_not_changed` flaky, so the checker should push the code toward
testable controls instead of guessing silently.

## Build order

1. Skill: generate interaction-state previews (param-driven). No plugin change. **Done 2026-11-18.**
2. `SemanticsDump`: add `selected` (and `enabled`) so detection and `state_not_announced` work.
3. Checker: `state_not_announced`; run all rules on every state.
4. Renderer: action-driven cases and `state_not_changed`.