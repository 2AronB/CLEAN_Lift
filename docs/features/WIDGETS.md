# Widgets — current implementation

## Authorization and boundary

User authorizes the Widgets tools and their saved results/progress in Metrics (2026-10-07): general and tempo timers, PACER/HAMR, current standard Air Force practice assessment, and five personal benchmarks including Cooper/Rockport estimates. All tool controls, setup, guided runners, target exploration, protocol instructions and attempt history are in Widgets. Metrics only gains the authorized stored measurements/results/trends. Other tabs, session builder, shared equipment and a standalone References tab remain outside this work. No new dependencies, permissions or services.

## Calculators

Three entries: Plate calculator, Pace & speed, Target splits. Plate calculator consolidates **%Max / Target weight (fresh default) / Warm-up loads**. Results immediately follow the active inputs; direct target results do not repeat the target. Independent remembered Home/Gym AND lb/kg equipment inventories; lower Equipment submenu has Save/discard handling. Legacy location-free inventory remains Home-only fallback; explicit empty saved inventories override it and Gym never inherits it.

Finite symmetric plate solver uses exact integer thousandths, bounded 200,000-state coin change and closest lower/higher alternatives. Duplicate sizes combine, odd spares are unused, bar-only is supported. Load calculations run off the main thread. Drafts survive mode/tab switches and rotation. No automatic workout weights or template equipment changes. Pace/splits retain exact mile conversion, partial final segments and bounded validation.

Ownership: widgets/WidgetCalculations.kt, WidgetEquipment.kt, WidgetCalculatorModel.kt, PlateCalculator.kt, PlateCalculatorScreen.kt and WidgetsScreen.kt.

## Timers

- General: countdown/rest, stopwatch/laps, work/rest intervals, EMOM and AMRAP. Manual round/rep tally. Total bounded to 24 hours; lap history bounded to 200.
- Tempo: lowering / bottom pause / lifting / top pause. Zero phases skipped; fixed repetitions or continuous mode, plus 30–300 bpm metronome. Cued reps are not detected reps.
- Shuttle: separate **20 m PACER / 20 m HAMR**. Android document picker selects a locally stored matching official recording. Native MediaPlayer plays its actual cues; no invented generic schedule or redistributed recording. Observer judges arrivals. PACER 2010 ends after the second miss, even when separated; final score is entered from the official score sheet. HAMR resets warning count on success, stops after three consecutive warnings, and records the last successful shuttle number. Cutting/skipping a HAMR shuttle requires ending the test.

WidgetTimer.kt uses monotonic absolute phase boundaries. WidgetClockModel.kt stores elapsed/tally/laps and interruption state in SavedStateHandle. Restoration is paused, not cue replay. VisibleWidgetClock keeps the visible screen awake; leaving/locking marks interruption and pauses. Fast tempo polls at 20 ms, other clocks at 100 ms; a delay over 1.5 s or skipped phase transitions pauses instead of catching up. Sound uses media volume. ShuttleAudioModel.kt preserves interrupted/ended/position state after recreation; interrupted recordings require a new attempt. Recording replacement resets the observer tally.

These are visible-screen tools. Background/screen-off timing and durable named presets are deferred. No automatic timers were added to Workout. Phone sound accuracy remains a manual check.

## Air Force 2026 standard PFRA

Widgets > Assessments provides **Quick score / Run practice test / Target explorer**. Explicit age at test (17–120) and M/F scoring table; profile settings never silently choose a table. Seven supported exercise modalities: one-minute push-ups or two-minute hand-release push-ups; one-minute sit-ups, two-minute cross-leg reverse crunches or forearm plank; two-mile run or 20 m HAMR. Nine age bands and both table sexes, 50 cardio + 20 body + 15 strength + 15 core points.

Primary numerical source: [AFPC PFRA scoring charts](https://www.afpc.af.mil/Portals/70/documents/FITNESS/PFRA%20Scoring%20Charts.pdf), cao 17 March 2026, pages 1–8. Rules: [official AFMAN 36-2905, 24 March 2026](https://static.e-publishing.af.mil/production/1/af_a1/publication/afman36-2905/dafman36-2905.pdf), §§3.7, 3.15 and Attachment 2. Both links and the [AFPC resource page](https://www.afpc.af.mil/Career-Management/Fitness-Program/lookatthescorecharts/) are in the app. Official hosts blocked direct retrieval; the same government-authored PDFs were retrieved from [5th FSS manual copy](https://5thforcesupport.com/wp-content/uploads/2026/04/AFMAN-36-2905.pdf) and [765th FSF chart copy](https://lajes765fsf.com/wp-content/uploads/2026/04/Tab-2.-PFRA-Scoring-Charts-cao-17-Mar-26.pdf). Two independent PDF extractors agreed on all 3,096 thresholds/172 rows; rendered push-up, plank, run and HAMR charts were inspected. Source SHA256 is in AirForceTables.kt. Age 17 support follows the under-25 table and [official minimum enlistment age](https://www.airforce.com/how-to-join/join-the-air-force).

Height is entered already recorded to nearest half inch. Three closest midpoint-waist readings must agree within one inch after each is rounded down to half an inch; average then rounds down again. WHtR scoring truncates to two decimals using exact decimal arithmetic. Raw ratio from the recorded measurements remains separate in Metrics. Example: 39.5 / 72 = 0.5486, scoring ratio 0.54, 15 body points.

Guided order is body first, then strength → core → cardio. Minimum five-minute rest gates start at actual event finish, including while entering results. The next event requires a deliberate start after recovery. Fixed events auto-finish at their duration; plank caps at the chosen table maximum; two-mile run ends manually. Observer announces the 15-second tone cues and judges repetitions/form. Physical components must finish within three hours. Pause/lifecycle/audio interruptions and partial completion are retained as deviations. Run instructions include up-to-three-minute warm-up and approximately-five-minute cooldown. HAMR uses the locally selected approved recording.

Target explorer locks the other components and searches actual thresholds for one selected strength/core/cardio result, with all physical minima met. No interpolation or hypothetical attempt saving. Impossible targets are explicit. Quick score distinguishes physical minima from composite ≥75; no inferred Excellent classification at the manual's ambiguous 90-point boundary.

Scope: standard primary WHtR assessment, non-exempt course below 5,250 ft. No medical exemptions, alternate 2 km walk, secondary BFA, altitude correction, special warfare/EOD or command-specific waivers. A secondary BFA may affect an official outcome when high WHtR and primary score fail. App results are practice calculations; official administration/myFitness remain required.

Ownership: AirForceTables.kt, AirForceScoring.kt, AirForceScreen.kt; shared clock/audio/attempt components.

## Five personal benchmarks

1. Navy Guide 5A two-minute push-ups — correct repetitions, terminating form/rest rules; 15-second tones for observer announcements. Practice only; Navy prohibits cellphone stopwatches for official PRT.
2. USMC strict dead-hang pull-ups — untimed, fully extended elbows to chin above bar, no assisted momentum/kipping; grip recorded in notes.
3. AF-form forearm plank — fixed personal benchmark cap 3:40 for everyone; not the age/sex assessment cap. Ends on loss of form/contact or third correction.
4. Cooper twelve-minute measured run/walk distance — fixed timer, metres including final partial lap. Early stop is incomplete. Outside-estimate results can still be saved as raw performance.
5. Rockport one-mile measured track walk — manual finish clock, immediate finishing pulse field next to timer, optional existing Kline estimate with age 20–69, M/F equation category, weight and pulse. No treadmill substitution.

Each has offline equipment/form instructions, source link, manual entry or guided timing, test date and notes. Paused/interrupted guided attempts are excluded from comparable trends; manually entered properly performed results remain eligible. Cooper/Rockport estimates reuse HealthCalculations; measured distance/time and estimated VO₂ stay separate. Invalid/unavailable estimates do not prevent saving a valid measured result.

Ownership: WidgetGuides.kt and BenchmarkScreen.kt. Source links use the normal browser, without app Internet permission.

## Storage and Metrics crossover

Additive Room schema **7→8** creates only widget_attempts, with unique Save token, protocol/date index and nullable linked-health foreign key (ON DELETE SET NULL). Prior workouts, sets, templates, body measurements and health records remain untouched. Save retries are idempotent; raw attempt + optional health result commit atomically. Original result, date, source version, entered/guided mode, human-readable input snapshot and deviations are immutable. Correct by deleting and re-entering in Widgets > Saved attempts; deletion also removes that attempt's linked health record if still present. Unrelated records are preserved.

Metrics > Test benchmarks includes PACER, HAMR, push-ups, strict pull-ups, plank and Air Force practice score. Complete comparable results only; failed scores still count as complete results. Incomplete/interrupted attempts stay in Widgets. Cooper/Rockport reuse the existing Cardio raw-distance/time series; linked estimates are deduplicated. Negative point IDs represent widget-origin rows in those two combined charts, preventing collisions with legacy positive health IDs. Raw widget rows remain inspectable and are corrected in Widgets; legacy health rows retain their existing editor.

Optional Cooper/Rockport VO₂ estimates use existing HealthKind/formula version 1. AF raw midpoint WHtR uses the existing WAIST_HEIGHT kind and cm snapshots, independently of scored truncation. A valid completed body measurement can link even if later physical events are incomplete/interrupted. No automatic overwrite of existing same-day results. Deleting an estimate directly in Metrics leaves the original widget performance intact. Edits to a linked health result never rewrite the original immutable attempt.

Ownership: WidgetAttempts.kt, WidgetAttemptModel.kt, WidgetAttemptHistory.kt; additive WorkoutDatabase migration/schema8 and authorized MetricsViewModel/MetricsScreen/MetricGuideScreen/MetricSeries integrations.

## Verification and manual acceptance

Build/JVM/lint and isolated emulator data/restoration tests are run with scripts/verify.ps1 (-DatabaseTests for emulator). Tests cover absolute timer boundaries, zero tempo phases, EMOM/AMRAP distinction, skipped fast cues, table maxima/minima/all age/sex transitions, official scoring examples, exact waist rounding/truncation, impossible target scenarios, rest/window deadlines, distinct shuttle warning rules, metric exclusion/deduplication, populated migrations 1–7→8, linked save rollback/idempotency/reopen/deletion, raw-body preservation and paused/audio process restoration. See docs/CONTINUE.md for final counts. No automated UI driving or phone audio acceptance.

Manual checklist:

1. Android Studio > open this project > Device Manager > start Medium Phone. Select that device in the top device dropdown and click the green Run triangle. Open Widgets; check categories and Back/draft/rotation behavior, small-screen keyboard, large text and dark mode. Existing plate equipment profiles must still reopen independently.
2. General timer: Countdown 5 seconds; Intervals work 3/rest 2 for 2 rounds (10 seconds); EMOM 2 rounds (2 minutes); AMRAP 10 seconds. Try stopwatch/laps/tally. Rotate, switch tabs and lock the phone during a run: it must pause/flag rather than replay cues.
3. Tempo: 1/0/1/0 for 2 reps = 4 seconds; metronome 120 bpm for 8 beats = 4 seconds. On a physical phone, check media-volume cues and timing against an independent clock. No screen-off behavior is promised.
4. Shuttle: outside the app, obtain the matching official 20 m recording and keep it on the device. Choose local official recording in Widgets. Observer marks arrivals after its cues, not during the introduction. HAMR miss/miss/success resets warnings, then three misses ends; PACER miss/success/miss ends. New recording/New attempt must clear counts. Leaving/locking during audio must mark interruption; saved attempt remains in Widgets and is excluded from Metrics trend. Check recording reopening after app restart.
5. Air Force Quick score: age 24, M, height 72, waist readings 39.5/39.5/39.5, push-ups 67, plank 3:40, two-mile run 13:25 → 95 points and scored WHtR 0.54. Save with WHtR enabled; Metrics should show raw ratio ~0.5486. Target explorer: variable Strength, target 90 → 52 push-ups with the other values locked. Try failed minima and impossible targets; no hypothetical Save.
6. Air Force guided: complete strength timer and enter reps; next component must remain disabled for five minutes from actual finish, even when entry takes time. Check core timer/plank cap, second five-minute rest and run/HAMR finish. End incomplete or pause: retain deviations and no comparable composite trend, while valid body measurement can still save. On a physical phone verify timing/cues; a full guided run is manual.
7. Save entered push-ups, pull-ups and plank; confirm Test benchmarks progress. Cooper 2500 m → estimate ~44.60 and one raw-distance point. Rockport 15:00, age 30, M, 72.5747792 kg (160 lb), pulse 110 → estimate ~49.04 and one raw walk-time point. Also save raw results without estimate context. Close/reopen; original inputs must remain. Delete one attempt; remove only its raw point/linked health result. Delete an estimate in Metrics; raw performance must remain.

Deferred: Army/Navy/Marine full test scoring, standalone References, session builder integration, shared Templates equipment, automatic Workout timers, named presets, and approved background/screen-off support. No new work outside Widgets plus the authorized Metrics/storage crossover.
