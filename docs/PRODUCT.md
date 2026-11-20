# Workout Logger — current product baseline

Consolidated 2026-10-15 from existing checkpoints. This describes the prototype, not new scope or new manual acceptance. Original instructions remain in [archive](archive/AGENTS-2026-10-15.md).

## Purpose

A fast native Android utility for recording strength training and reviewing local records. Kotlin/Compose/Material 3/Room; fully offline, no accounts, ads, subscriptions, analytics, tracking, backend or Internet permission. Minimal dependencies; changes to dependencies/permissions/services require approval. Correctness and durability precede features.

## Current flows

Standing direction (2026-10-16): build the comprehensive, data-backed workout app Android should have shipped with, free of ads, distractions and exploitation. Proactively improve common start/log/review/repeat flows in complete small slices; use progressive disclosure for specialized tools. Existing offline/privacy/durability and explicit dependency/permission/service approval boundaries remain. Pending manual acceptance does not bar independent safe development. This authorizes prioritization, not unsupported claims of population demand or personalized training efficacy.

| Area | Implemented behavior | Contract |
|---|---|---|
| Workout | Empty/template start, saved sets, explicit previous values, guided order/overrides, resume, finish/cancel | [Architecture](ARCHITECTURE.md), [template convenience](features/TEMPLATE-CONVENIENCE.md), [data](LOCAL-DATA.md) |
| Templates | Exercise library/custom edits, ordered plans, editable duplication, equipment-aware draft generator | [Generator](features/WORKOUT-GENERATOR.md), [catalog](features/EXERCISE-CATALOG.md) |
| Metrics | Completed history/corrections, daily/single body records with native dates, method-specific health/cardio estimates, strength/Big 3 and integrated 5/10-rep tools, comparisons/preferences | [Metrics](features/METRICS.md), [progress](features/PROGRESS.md), [measurements](features/MEASUREMENTS.md) |
| Widgets | Load/pace calculators, visible-screen timers/tempo, local-recording PACER/HAMR, AF practice assessment and five personal benchmarks; documented Metrics crossover | [Widgets](features/WIDGETS.md) |
| Settings | Local preferences/profile, CLEAN joke controls, versioned local JSON backup and confirmed replacement restore; delete-all disabled | [Settings](features/SETTINGS.md), [portability](features/PORTABILITY.md) |

Tabs (D-012): Workout / Plans / Progress / Tools, labels for the internal Workout / Templates / Metrics / Widgets areas. Fresh launch opens Workout. Settings (with Appearance: theme mode and Iron/Chalk/Forest/Wallpaper palettes) opens from the gear on each tab root and Back returns to that tab. History is under Progress, exercises under Plans. Preserve origin, drafts and supported restoration behavior.

## Behavior to protect

- Saved sets retain original values/units; unfinished workouts resume. History excludes active sessions.
- Template edits never alter active/history snapshots. Renames retain stable IDs and historical names. Corrections preserve unrelated fields.
- Measurements use calendar days; workouts use timestamps. Body measurements allow one record per metric/day; health methods allow repeated results. Calculated results snapshot inputs/profile/formula, rather than silently following later edits.
- Strength views use stable selected IDs and compatible methods. Relative strength needs same-day bodyweight. Session totals differ from sums of personal bests on different dates.
- Widget attempts are immutable source records with idempotent saves. Incomplete physical attempts do not become completed comparable scores; the documented valid AF body-measurement exception remains. Timers pause/flag interruption; background operation is not promised.
- Local backup/restore preserves exact saved source data and settings, validates before replacement and recovers interrupted settings application. Unsaved drafts/timers/audio files are not portable; Android automatic backup stays disabled. See [portability](features/PORTABILITY.md).
- Schema 8 and preserving migrations 1–7→8 exist. Never reset production data to simplify changes. See [local data](LOCAL-DATA.md) and feature notes for exact contracts.

## Deferred or undecided

Candidates to prioritize under the standing direction, not promises of implementation: actual available load increments and machine identities; timed workout-set logging, background/audible rest alerts (a visible-screen rest countdown exists in Workout), supersets, RPE/notes editor, CSV/other export formats, other military branches, standalone References and more cross-tab integrations. Existing Widgets timers do not provide Workout integration. Adaptive progression/automatic numeric load prescription needs a reviewed evidence/behavior policy; backup merge/version conversion and Android automatic backup/transfer need a reviewed recovery/privacy policy. Background timers must honor explicit permission/service approval boundaries.

The approved five personal benchmarks and shuttle/AF results already exist. Older notes excluding all benchmarks or calling Widgets a placeholder are historical.

## Acceptance

Milestones 0–5 were manually accepted in earlier records. Later features have automated verification with outstanding manual UI/audio/phone checks. [CONTINUE](CONTINUE.md) holds dated evidence; no new acceptance is inferred here.

Core durability test: create an exercise, start a workout, save several sets, finish, close/reopen, and see exactly the entered history. Feature acceptance also covers failed saves, cancellation, changed drafts, Back, rotation, keyboard, dark theme and large text as applicable.
