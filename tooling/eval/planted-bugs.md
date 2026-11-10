# Planted bugs (answer key)

The ground truth for the eval. The tool and the agent must never read this file.
The sample sources carry no hint comments; bugs are documented only here.
A run passes when every bug is reported, with no false positive on the controls.

| ID   | Component        | Exposed in state | Bug                                                         | Expected signal from semantics/pixels                                   |
|------|------------------|------------------|-------------------------------------------------------------|-------------------------------------------------------------------------|
| OS-1 | OrderSummaryCard | long_text        | Item name has no weight, so it squeezes the price            | Price text node width collapses / `visualOverflow` on price             |
| OS-2 | OrderSummaryCard | all              | Remove icon is a 24dp touch target                          | Clickable node with bounds < 48x48dp                                    |
| OS-3 | OrderSummaryCard | all              | Remove icon has no accessible label                         | Clickable node with no text/contentDescription in itself or descendants |
| OS-4 | OrderSummaryCard | font_2x          | Total text has fixed 24dp height                            | `visualOverflow: true` on the total text                                |
| OS-5 | OrderSummaryCard | all              | Pay button is 36dp tall                                     | Clickable node height < 48dp                                            |
| PH-1 | ProfileHeader    | rtl              | Absolute arrangement/padding, so the row does not mirror    | Avatar x-position identical in LTR and RTL                              |
| PH-2 | ProfileHeader    | dark             | Name color hard-coded near-black                            | Text/background contrast < 4.5:1 in dark render                         |
| PH-3 | ProfileHeader    | default          | Subtitle #C4C4C4 on white                                   | Text/background contrast < 4.5:1                                        |
| SR-1 | SettingsRow      | font_2x          | Fixed 56dp row height clips text                            | `visualOverflow: true` or unclipped > clipped bounds on texts           |
| SR-2 | SettingsRow      | long_text/font_2x| Description is single-line ellipsized                       | `ellipsized: true` on description                                       |
| SR-3 | SettingsRow      | all              | Row is clickable with no Role/toggle state                  | Not visible in semantics (Switch with null callback has no state). Visual review only; skill step 5 asks for it |

## Controls (must NOT be flagged)

- `ProfileHeader` Edit `TextButton`: Material button with min touch target and text label.
- `Switch` in `SettingsRow`: has toggle state; the issue is the parent row (SR-3), not the switch.

## Scoreboard

| Run | Date       | Found | Missed | False positives |
|-----|------------|-------|--------|-----------------|
| 1   | 2026-11-10 | 10/11 | SR-3   | Edit TextButton touch target (fixed: layout size); SettingsRow dark contrast (fixed: previews wrap Surface) |
| 2   | 2026-11-10 | 10/11 | SR-3   | none |
| 3   | 2026-11-10 | agent `/ui-verify SettingsRow`: fixed SR-1, SR-2, SR-3, report pass | - | **Invalid as an agent eval**: source still had `// PLANTED` comments naming the fix. Comments removed; rerun needed. Agent output kept in `runs/run3-agent-SettingsRow.kt` |
