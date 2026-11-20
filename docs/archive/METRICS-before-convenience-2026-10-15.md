# Metrics and five-tab navigation

User request (2026-10-07) replaces the former four-tab layout. Root tabs are Metrics, Templates, a larger center Workout icon, Widgets, Settings. Workout is the fresh-launch default. Exercises live inside Templates; History is reachable from Metrics and is limited to workout review/corrections. Lift estimates (the older 5/10-rep and break-scenario tool) is reachable directly from Metrics, not nested in History. Saveable tab state retains navigation/drafts through switching and rotation. Widgets now has the tools documented in WIDGETS.md.

## Authorized Widgets results (2026-10-07)

Additive schema 7→8 stores immutable widget_attempts and optional linked health snapshots. Metrics gains comparable completed PACER/HAMR, push-ups, strict pull-ups, plank and 2026 Air Force practice-score trends. Tool controls, entry, attempt corrections/deletion and timing remain in Widgets. Partial/interrupted physical tests stay there; failed-but-completed practice scores still appear in progress. A valid AF body measurement may save raw midpoint WHtR even if later physical events are incomplete. The raw ratio is separate from truncated PFRA scoring.

Cooper/Rockport retain their existing Cardio raw-distance/time charts; WidgetAttemptRules.integrate merges legacy health rows with immutable widget-origin rows without duplicating linked estimates. Negative point IDs distinguish raw widget rows from positive legacy health IDs. Widget-origin rows are corrected in Widgets; existing health rows retain their editor. Estimates use existing HealthKind.COOPER/ROCKPORT, and direct estimate deletion preserves raw widget performance. Deleting a widget attempt removes only that attempt and its still-linked health record. Profile or standards changes never rewrite attempt snapshots. Details/protocols/sources and manual checks: WIDGETS.md.

## Metrics usability review (2026-10-07)

- Workout history has a visible Back button at list level and no longer duplicates measurement/progress navigation. Existing exercise estimates remain available through Metrics > Lift estimates, including chart-to-workout-to-chart return state.
- A body metric's Add measurement opens the form directly; tapping a saved row opens that exact record. Save/delete/discard returns to the original Metrics chart. New forms honor the Metrics lb/in or kg/cm preference; corrections retain the stored entry's original unit. Deleted records cannot become new entries, and an in-flight deletion can finish its success/reset handling.
- Overview adds saved category and recorded-data-only filters with an explicit empty-filter recovery action. These are browsing filters, independent of persistent visibility preferences; they do not delete data. Existing ordering and chart/overview scroll restoration remain.
- Comparisons resolve distinct visible metrics when saved selections become hidden, disable empty pickers, explain insufficient choices and explain why percentage change cannot use a zero baseline. Four regression tests exercise comparison fallback/selection. Percentage-baseline guidance is shown only in percentage mode.
- Metrics date fields now have keyboard padding. Long widget inputs expand when selected; details/protocol/research stay in About & research instead of being repeated after every chart list. Big 3 configuration remains directly reachable above its chart.
- No schema, repository/query, dependency or permission changes. Automated results are recorded in CONTINUE.md; visual/navigation/draft acceptance remains manual.

Focused manual acceptance:

1. In Android Studio open this project, click Device Manager, start the existing desktop emulator, select it in the top device selector, and click the green Run triangle.
2. Metrics > Workout history: confirm only search/workouts appear. Open a workout, correct one set, then use Back twice to return to Metrics. From a chosen strength chart, open a workout row and return; its filters/scroll must remain and the corrected value must update.
3. Metrics > Settings: turn Use lb / inches off. Back > Body measurements category > Bodyweight > Add measurement: expect kg and a form immediately. Save a dated value; expect return to the chart. Tap its row, confirm the exact date/value, correct it, save, and reopen that row. Older lb records must open in lb without reinterpreting the number.
4. In a measurement editor change a value, press Back, choose Keep editing, rotate, and confirm the draft remains. Press Back > Discard to return to the chart. Delete a test measurement and immediately add another; the new form must remain usable. Try a duplicate metric/date and confirm the existing record remains with a clear error.
5. Filter by category and Only metrics with recorded data. Return from a chart and confirm browsing filters remain. An empty filter must offer Show all visible metrics. Hiding a group in Settings must still exclude it without deleting records.
6. In Compare choose two metrics; hide the selected first one in Settings and return to Compare. The two resolved selections must differ. With only one visible metric there must be one chart and a request to show another. A benchmark whose first result is zero must explain unavailable percentage change and still show raw results when that switch is off.
7. Metrics > Lift estimates: open a lift, select a historical workout row, correct a set and return; the lift selection must remain. Select a widget result row to reveal its saved inputs. About & research must remain reachable above the chart, including offline instructions.
8. Check custom-date typing with the keyboard visible, large text, dark mode, and close/reopen persistence. Visual comfort and phone behavior require your acceptance.

Suggested follow-ups (not implemented): native date pickers for body/health entries and custom ranges; a single daily-measurement form that saves several raw measurements together; consolidating Lift estimates into the chosen-lift detail without losing its break scenario; explicit local backup/export before wider use.
## Ownership

- ui/WorkoutLoggerApp.kt: tabs and saved tab state.
- ui/MetricsScreen.kt, ui/MetricsViewModel.kt: catalog/overview, visibility/order/units/reference/average settings, date filters, comparisons and charts.
- ui/HealthEntryScreen.kt: metric-specific entry, instructions, profile defaults, draft/discard/delete handling.
- ui/MetricGuideScreen.kt: one-tap equipment/protocol/formula/interpretation/research screen, accessible above long result lists.
- data/HealthMetrics.kt: canonical inputs, durable snapshots, formula version 1, calculations and reference labels.
- data/HealthGuides.kt: offline equipment/protocol/formula/interpretation descriptions and external sources.
- data/MetricSeries.kt: method-separated chart projections, fat/fat-free mass and recorded-weight averaging.
- data/StrengthMetrics.kt, data/ProgressDao.kt, ui/StrengthPreferencesScreen.kt: completed-workout samples, chosen stable-ID benchmarks, rep-specific/estimated/relative charts, and explicitly mapped Big 3 totals.
- ui/TrendChart.kt: numeric chart and point selection; readable exact-value rows remain the accessible alternative.

The overview shows latest values/dates, miniature raw-data trends and change from the first record. Strength/Big 3 detail screens show the all-time highest value/date independently from date-range filtering. Metric detail provides raw chart points, reference markers where supported, exact rows and one-tap About & research. Comparisons align dates without dual-axis ambiguity; optional percentage-change baselines retain their own recorded starting dates.

## Data safety

Schema 7 adds only `health_records` and its nonunique kind/day index. Migration 6→7 changes no existing table. Populated older migration tests now include 6→7. Health records permit multiple results on a day (repeated tests are legitimate); body measurements still allow one entry per metric/day. Adding HEIGHT to BodyMetric changes no table/schema representation; prior enum values retain their names.

Health records persist the canonical cm/kg inputs, age at measurement, profile M/F/Not set snapshot, pulse/time/distance, notes and formula version. Profile changes never recalculate old results. Editing a result deliberately recomputes only that record from its saved/edited inputs; kind cannot change. Deletion is record-scoped. Input validation is repeated inside the transactional repository. Workout history is never rewritten.

Calculated indices are explicit saved snapshots, not automatically refreshed derivatives of later edited body measurements. New forms prefill same-day body measurements and the latest height on/before that date; users verify landmarks. Date changes retain drafts; an explicit Load measurements button refreshes matching inputs. Navy/RFM waist measurements are not assumed from legacy WAIST because their required sites differ. All inputs remain visible in the result list/editor. Profile age is a starting value; users correct it for retrospective results.

Private `metrics-page` preferences control individual/group hiding, order, imperial/metric units, sourced reference context and optional weight average. Group hiding preserves individual choices and applies to newly selected lift series; comparisons respect hidden metrics/groups. Hidden data is retained. Comparisons do not mix methods or interpolate missing records. Fat-free mass is explicitly not muscle mass. The average uses recorded values in the seven calendar days ending on each measurement date; it is shown separately from raw values. Overview/settings/detail list scroll state is saved separately; About & research and history return to the originating chart.

## Formulas and references

Strength benchmarks are selected explicitly; Big 3 mapping automatically exposes their individual series. Rep-specific records support a selected 1–30-rep count. Epley estimates in this new view use only positive 2–10-rep sets; logged singles are separate and not automatically claimed as true maxima. Existing History exercise progress retains its original 5/10-only semantics. Relative strength uses only bodyweight recorded on the same local calendar day as the workout, with the date and denominator shown in the list. No future/current/stale weight is substituted.

Big 3 requires three distinct mapped exercise IDs; no name-based inference or remapping. Same-session totals require all three values in one completed workout. Running personal-best sums may combine different workouts and are labeled accordingly, with component loads/dates shown in the rows; later records cannot leak into earlier points. Estimated and logged-single modes remain separate. History corrections update these query-derived projections; selected metric/date/unit state is retained when returning from a workout. Rep count, selected benchmarks and Big 3 mappings persist in the private metrics preferences. Every series can be hidden individually.

- BMI: kg/m²; CDC adult categories only at age ≥20.
- Waist/height: same-unit ratio; NICE adult ranges with age and BMI-scope checks. Below 0.4 is not labeled superior health.
- Waist/hip: same-unit ratio; WHO adult screening context and profile gender.
- Historical Navy/Hodgdon–Beckett: M navel abdomen/neck/height; F natural waist/hips/neck/height, converted to inches before logs. Not current Navy policy or pass/fail scoring.
- Relative fat mass: `64 −20 × height/waist +12 if F`; NHANES hip-crest waist protocol. Proposed RFM mortality-study thresholds are distinct from universal body-fat cutoffs.
- Manual body fat: DXA, impedance, skinfold and other remain separate series; optional same-session weight yields estimated fat/fat-free mass.
- Rockport: exact one-mile level track walk, time m:ss, finishing bpm, weight, profile gender and age 20–69; equation uses lb and decimal minutes. Treadmill walking is not accepted as equivalent protocol.
- Cooper: distance in metres during exactly 12 minutes; raw distance retained alongside the estimate. Adult field estimate with population limitations explained.
- Laboratory treadmill and device/other VO₂ inputs remain separate. FRIEND 2015 Table 3 percentile bands use saved age/gender, ages 20–79; estimated field/device comparisons are labeled approximate rather than measured CPX categories.

Source pages are linked within each metric and opened using the normal browser. Offline instructions/calculations are bundled. No embedded web, Internet permission, telemetry, dependencies or sensors are introduced. Unsupported universal “athletic” or personal mortality labels are not invented. Source associations are population context, not individual diagnosis.

## Manual acceptance (not yet accepted)

1. Open Android Studio and this project. In Device Manager start the existing desktop emulator, select it in the top device selector, then click the green Run triangle.
2. Confirm tabs left-to-right Metrics / Templates / larger Workout / Widgets / Settings, and a fresh launch opens Workout. Templates > Exercise library must retain create/edit/search. Confirm generator/template start still enters Workout.
3. Settings > Edit profile: set age 30 and gender M. Metrics > Height > Add measurement: save 180 cm today. Save bodyweight 81 kg, waist 90 cm and hips 100 cm on the same day.
4. Metrics > BMI > Add result: verify prefills, save and expect 25 (CDC Overweight boundary). Waist-to-height: confirm midpoint protocol, add height 180/waist 90/weight 81, expect 0.5 and Increased context. Waist-to-hip 90/100 gives 0.9.
5. Navy tape: height 70 in, neck 15 in, abdomen 34 in gives about 17.51%. Change profile to F; add height 64 in/neck 13/natural waist 29/hips 39, expect about 29.21%. Old M result must remain unchanged. Existing result editor retains the saved gender unless Use current profile gender is selected.
6. RFM: M, height 180 cm/hip-crest waist 90 cm gives 24%; F gives 36%. Record a DXA percentage and weight, inspect separate fat/fat-free mass charts. Missing weight must not invent mass.
7. Read Rockport equipment/protocol first. Enter age 30, M, 160 lb, 15:00, finishing 110 bpm; expect about 49.04 mL/kg/min. Missing pulse/age/gender or 15:60 must prevent saving. Cooper 2,500 m in exactly 12 min gives about 44.60; inspect raw distance/time series.
8. Inspect charts, tap points, edit a result and delete another; close/reopen and confirm remaining data. Test two results on one day, mixed displayed units, custom date ranges and comparison baselines. Raw weight remains alongside any average.
9. Metrics > Settings: hide one metric and a group, reorder, change units/reference/average, close/reopen and confirm preferences. Show all restores visibility without changing records.
10. Verify browser research links, offline operation, dark theme, large text, TalkBack numeric lists/controls, rotation and unsaved-back confirmation. Widgets must retain its implemented calculators, timers and benchmark tools. Existing workout history must be unchanged.
11. Metrics > Choose lifts: select a lift and map distinct Squat/Bench/Deadlift IDs. Confirm each chosen lift gets six views and Big 3 has four separate total views. Change the rep-specific count and confirm only matching logged sets contribute. Add same-day bodyweight for relative strength; unmatched days must have no point.
12. Log the three lifts in separate completed workouts: personal-best sums may appear, while session total stays empty. Log them in one workout to enable session total. Singles and 2–10-rep estimates must remain distinct. Tap a chart record, correct its workout set, press Back and confirm the chart updates and keeps filters. Active workouts must not contribute.
13. From a long metric list open a chart and confirm it starts at the top; return and confirm overview scroll is restored. About & research must be reachable above the chart. Hide Strength, select a new lift and confirm its metrics stay hidden until the group is shown. Hidden metrics must not appear in comparison selections or default charts. Verify Big 3 component dates correspond to the actual source workouts.

Automated checks and final checkpoint are recorded in docs/CONTINUE.md after verification. Visual and subjective acceptance remains the user's responsibility.

## Requested convenience changes — 2026-10-15

Status: approved for implementation by “Implement your four suggestions”; automated/manual verification pending.
Scope: native date pickers for body/health entry and custom chart ranges; one daily form for multiple raw body measurements; legacy lift estimates/scenario within chosen-lift detail; local export. Restore scope is being clarified before portability implementation. Android automatic backup stays disabled.
Acceptance: dates preserve calendar days and reject future dates; batch blank fields are skipped and conflicts cannot partially save or overwrite; chosen-lift estimate filters/scenario survive history round-trips; export contains version metadata and exact source data, reports failed/cancelled writes honestly and never mutates the source.
Data: no schema change anticipated. Batch saves require a single Room transaction with repository validation and isolated rollback/reopen tests. Estimates keep existing method semantics; no formula or reference changes.
UI: preserve changed drafts, scroll/tab/navigation origin and existing single-record corrections. No dependencies, permissions, services or app networking.
Recovery: preserving source data and forward fixes; no uninstall/reset. Export/restore format and limits will be documented in a dedicated portability contract.

Date-picker slice: build/102 JVM tests/lint passed 2026-10-15. Manual calendar acceptance pending. Backup scope clarified by user: include restore.
