# Workout generator implementation

Implemented under Templates > Generate workout, updated 2026-10-07. Ownership: `data/WorkoutGenerator.kt` (deterministic selection), `GeneratorRepository.kt` (transactional save), `ExerciseCatalog.kt` and `CatalogRepository.kt` (bundled metadata/install), `GeneratorLocations.kt` (private equipment preferences), `ui/GeneratorScreen.kt`, templates/workout repositories/entities and their screens.

## Behavior

Presets: Full body A/B, Pull/Push/Legs, Bodyweight A/B, 5×5 main lifts A/B and Kettlebell strength A/B. Latest completed generated session in the same style suggests the next session by completion timestamp/ID; active/cancelled/untagged workouts do not advance it. No calendar required. Preview/save does not advance the rotation. Manual template edits clear generator provenance; completed workout snapshots retain it.

Maximum main exercises uses a discrete slider from 4 to 10. This is a ceiling: styles and limited equipment can produce fewer exercises. Required movement slots precede optional work in selection. Curated staples win before alternatives; this is an editorial/user-requested preference, not a measured population-frequency ranking. Additional slots cover single-leg work, calves, shoulders, arms, hip abduction/adduction and trunk where suitable. Do not add filler to reach the limit.

Compound-first defaults on. After selection, a stable ordering puts opted-in ballistic swings first, compound squat/hinge work next, other compound/integrated work next and isolation last. Switch off to retain the preset sequence. This uses transparent curated categories, not an invented quantitative measure of muscle stimulus. All drafts can be reordered manually.

Gym and Home have independent remembered equipment selections. Both default to clear floor space, with no inferred gym machines. The former single equipment selection migrates once to Home; invalid/obsolete enum names are ignored and intentionally empty selections remain empty. Selected location is remembered. These are local private preferences, not a schema change or a calendar schedule. Available weight ranges/increments and machine identities remain future work.

Equipment groups are AND within a group and OR across alternatives. A dumbbell pair also permits single-dumbbell work. Practiced-lift inclusion stays optional and off by default; swings have a separate explicit familiarity opt-in. No Olympic lifts, timed holds or distance carries are generated. Kettlebell strength uses only selected bell/floor equipment; bodyweight fallback is permitted. Bodyweight presets use rep-only entries. 5×5 permits total-external-load main lifts plus bodyweight alternatives: only designated loaded main lifts get 5×5 targets.

Missing equipment uses an eligible bodyweight alternative for the slot. Floor-only pulling uses Prone W Raise (and T Raise for complementary upper-back work), labelled explicitly as lighter upper-back accessory work. It is **not** credited as a loaded row/pull-up or equivalent lat coverage. Low fixed bar plus clear floor space enables inverted rows without the technical-lift opt-in; pull-ups still require an actual bar and practiced-lift opt-in. No furniture/door anchors or equipment are invented. If a basic floor alternative is horizontal rather than vertical pushing, the coverage limitation is stated. Missing floor space still produces an honest coverage note and can yield an empty draft. Notes use ordinary informational text rather than red validation errors.

Main keys/families are normally deduplicated. A required slot may use a distinct bodyweight variant from an already-used family as a last fallback; exact main exercise keys remain unique. Isolation does not silently satisfy missing primary muscle coverage.

Targets are editable product starting presets, not personalized prescriptions: main work usually 3×6–10/rest 120s, secondary 2×6–10/rest 60s; effort 1/2 uses 1/2 working sets, effort 3/4/5 secondary work uses 2/3/4 sets. Loaded designated 5×5 lifts retain five sets of five/rest 180s; bodyweight substitutes use ordinary reps and a clear note. Explicit swings use 10–15 reps. No warm-ups, automatic load suggestions or overload rules are added. Weight conventions/per-side reps are shown; bodyweight weight defaults to zero. Previous-value references remain explicit user actions.

## Ab finisher and persistence

Optional ab circuit adds three floor exercises after all main work: dead bug, reverse crunch, bicycle crunch. Two rounds are expanded into six separate ordered rows, each initially one set of 8–12 reps. Rest targets are 15 seconds between moves and 60 seconds at each round end. The finisher is additional to the main-exercise ceiling. The ordinary trunk slot is omitted when a finisher is requested. No automatic timer/countdown is provided; timed HIIT/EMOM remains unsupported.

Nullable `circuitRound` on template/workout exercise rows preserves round identity. Existing guided logging already handles each ordered row and its one-set target; repeated exercises in different rounds share catalog exercise IDs but have different workout entry IDs. Round labels appear in draft, ordinary template builder, active workout and history. Editing/duplicating ordinary templates preserves targets/rounds; manual reorder/removal/replacement may change the circuit or coverage and is explicit. Replacement of a circuit row is limited to compatible primary abs/obliques work; duplicate key within the same round is rejected.

Schema 5→6 adds only nullable circuitRound INTEGER columns on template_exercises and workout_exercises. Old fields/IDs/sets/notes/provenance remain intact; pre-existing rows get null. Schema 4→5 metadata migration remains unchanged. Exported schemas 1–6 retained; no destructive fallback. Templates snapshot names, targets, catalog identity, rounds and generator session into workouts, preserving history through later edits. Targets do not restrict actual logged reps.

## Exercise tab

Catalog v3 has 69 variants (16 added): inverted rows, W/T raises, reverse/bicycle crunches, bird dog, pike push-up, bodyweight calves/hip work, DB pullover/rear fly/triceps/wrist work and kettlebell curl/halo. The 66 variants compatible with load/reps or reps-only logging are installed into the Exercises tab and all ordinary pickers immediately, not only after generation. Search and primary-muscle/equipment detail are available in Exercises. Time/distance entries await corresponding logging support.

Installation is version-marked and transactional. Already-mapped keys reuse stable IDs, including user-renamed variants. Existing legacy/custom exercises and history are never auto-linked by names. A catalog-name collision receives a separate labelled variant; existing IDs, names and previous values remain unchanged. Old starter rows remain available. Generated save reuses installed IDs or inserts missing variants inside its template transaction. Preview writes no exercises/templates/history. Duplicate-save failure rolls back additions and retains the draft.

## Evidence and limits

Metadata and selection weights remain curated biomechanical/product judgments. They are not experimentally validated coefficients or guarantees of sufficient stimulus. Primary, secondary and stabilizer tags remain distinct. Counts/targets/order are editable defaults, not individualized medical advice.

[ACE scapular stabilization library](https://www.acefitness.org/resources/everyone/exercise-library/249/prone-scapular-shoulder-stabilization-series-i-y-t-w-o-formation/) identifies prone W/T work as shoulder/back stabilization without equipment. Its demonstrations include holds; our rep-counted raise variants and generic targets are editorial adaptations, not the site's exact protocol. The distinction from resisted lat pulling is a biomechanical inference reflected in separate movement and primary-muscle tags.

Earlier kettlebell review: [ACE getting started](https://www.acefitness.org/resources/pros/expert-articles/5269/how-to-get-started-with-kettlebells/), [ACE introductory discussion](https://www.acefitness.org/continuing-education/certified/october-2018/7097/women-and-kettlebells-better-together/) and [Men's Health examples](https://www.menshealth.com/fitness/a19528447/ultimate-kettlebell-workout/). These suggest recurring squat/hinge/push/pull families and varied formats; they do not validate our exact routine. See EXERCISE-CATALOG.md and EXERCISE-CATALOG-RESEARCH.md for broader taxonomy/references. No copied routines, photos or network access in the app.

## Verification and manual acceptance

Verified 2026-10-07: build, all 43 JVM tests and Android lint passed (zero errors, 22 warnings); all 33 isolated database/preference tests passed on emulator-5554. Final metadata/help-text/history-label refinements also passed build/JVM/lint afterward; no schema changes since the database run. Tests include max 10, stable ordering toggle, every style/session with floor-only fallback, genuine inverted-row coverage, finisher rounds/rest/order, skill gates, effort, history rotation, catalog installation/rename/idempotency/history preservation, independent Gym/Home preferences, generated save rollback/reopen and populated versions 1–5 migrating to 6. Production workout-logger.db is never cleared. No new dependency, permission or Internet permission.

Manual acceptance remains pending:

1. In Android Studio open this project. Open Tools > Device Manager, start the Medium Phone desktop virtual device, select it in the top toolbar, and click the green Run triangle. If the test emulator is already headless, stop/start only that desktop emulator in Device Manager. Do not clear app storage.
2. Tap Templates > Generate workout. Configure Gym with your actual gym equipment; configure Home with floor space only. Switch between them, close/reopen the app and confirm each setup stays independent. Prior equipment should be retained under Home.
3. Generate Full body with maximum 10 and suitable equipment. Enable practiced lifts if familiar. Confirm compounds precede accessories. Turn the ordering switch off and confirm the preset order; test manual reorder/replace and save.
4. Generate Bodyweight at Home with floor space only. Confirm Prone W Raise appears and the informational note explains the lat gap; there should be no red missing-pull error. Add a suitable low fixed bar and confirm inverted row is chosen. Clear all equipment and confirm the honest empty-draft behavior.
5. Enable Add ab circuit, generate and save. Confirm three moves in round 1 followed by the same three in round 2, at the end. Start it, log/advance sequentially, close/reopen mid-round, finish and inspect History. Confirm round labels, exact sets and separate occurrences survive.
6. Open Exercises; search Reverse Crunch, Inverted Row and kettlebell variants. Confirm library entries exist before saving any generated template and show muscles/equipment. Check old custom exercise/history names remain intact.
7. Check limited-equipment 5×5 substitutes show ordinary rep targets; check swing opt-in, draft rotation/discard, large text and dark mode. Rest targets are hints, not a countdown.

Future work: equipment weight inventories; explicit legacy/custom metadata association; fuller filters; accepted-target versus actual progression; timed logging/rest timer and kettlebell intervals.