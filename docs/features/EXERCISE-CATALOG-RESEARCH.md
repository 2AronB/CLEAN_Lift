# Exercise survey and starting-load design

Research/design checkpoint: 2026-10-07. This is a proposed offline catalog and estimation policy, not implemented filtering or load recommendations. No runtime, schema, dependency or permission changes.

## Findings and evidence boundaries

Use independent facets rather than one category tree. Equipment, anatomy, movement, training purpose and skill answer different questions. ACE's library demonstrates equipment/body-region/experience browsing [S1]. Our precise taxonomy and row mappings below are editorial biomechanical classifications, not experimentally validated rankings. Primary/secondary muscle roles vary with technique; avoid invented percentages of muscle involvement or inferring hypertrophy from EMG alone.

HIIT describes repeated intense efforts with recovery, and can use multiple exercise modes [S2]. Store interval compatibility on exercises and HIIT on a session's prescription. Snatch and clean & jerk are Olympic competition lifts [S3]; their derivatives need separate labels. A front squat can support weightlifting without being an Olympic competition lift.

Repetition capacity at a percentage of 1RM varies between people and exercises. A large synthesis found different relationships for bench press and leg press [S4]. It does not establish cross-exercise conversion ratios. A small chest-press comparison found combined dumbbell 1RM about 17% below barbell 1RM [S5]; this is a population observation, not a reliable individual conversion or proof of a safe starting weight. ACSM's older progression guidance supports increases after demonstrated excess repetitions [S6], not deriving unfamiliar exercise loads from unrelated PRs.

## Proposed facets and controlled values

| Facet | Values / representation | Use |
|---|---|---|
| Equipment | Explicit requirement groups; every item in one group required, any complete group sufficient | A barbell alone does not satisfy bench press; distinguish flat/adjustable bench, rack/safeties, plates, pulley attachments and specific machines |
| Body region | Chest, back, shoulders, arms, hips/glutes, thighs, calves, trunk, full body; multiple allowed | Simple browser groups |
| Muscles | Primary movers, secondary movers, stabilizers in separate lists | Quads, hamstrings, glutes, adductors, calves, pecs, lats, upper back, deltoids, biceps, triceps, trunk; optional anatomical subgroups |
| Movement | Squat, hinge, horizontal/vertical push/pull, lunge/split squat, carry, rotation/anti-rotation, trunk flexion/extension, locomotion, jump; accessory joint actions | Pattern coverage and substitutions |
| Breadth | Compound / isolation / integrated | Broad vs targeted movement coverage; does not measure quality |
| Technical demand | Basic / moderate / advanced / unknown | Separate from breadth; Olympic lifts are compound and technically advanced |
| Purpose | Strength, muscular endurance, power, conditioning, mobility; multiple allowed | Capability rather than a promised training outcome |
| Discipline | General resistance, calisthenics, Olympic competition, Olympic derivative, cardio, plyometric, mobility | Independent from training purpose |
| Mechanics | Bilateral/unilateral/alternating, supported/unsupported, stance, grip, angle, range-of-motion variant | Prevent invalid substitutions and load transfer |
| Recording | Reps+load, reps-only, time, distance+time, carry load+distance | Block generation that current logger cannot represent |
| Load semantics | Total external, per implement, added bodyweight load, assistance, machine display, band identifier, unknown | Numeric values are comparable only within compatible semantics |
| Provenance | Source IDs, claim supported, review date, evidence class, catalog version | Distinguish anatomy/editorial decisions from measured ratios |

Equipment vocabulary starts with bodyweight/no implement, barbell, plates, rack, safeties, flat bench, adjustable bench, dumbbell pair/single, kettlebell, cable high/low pulley, handles/rope, pull-up bar, dip bars, machine subtype, bands and secure anchor, box, open floor, rowing ergometer and cycle. Store counts and available loads (including actual bar mass). Space/support requirements still matter for bodyweight exercises. Do not imply all cable stations or all machines are interchangeable.

For broad vs specialized browsing, expose two controls: **Movement coverage: compound / isolation / integrated** and **Skill: basic / moderate / advanced**. A third optional discipline filter handles sport specialization. Never collapse these into a single score.

## Survey of all 31 existing starter names

BB = barbell plus plates; DB = dumbbell; C = compound; I = isolation. Listed muscles are principal browsing targets, not exhaustive anatomy. Unless noted, these are general resistance exercises recorded with reps and load. This is a curated first pass, requiring variant-level source review before shipping numeric advice. S1 informs the browsing structure, not a claim that every row was individually verified against ACE.

| Current name | Explicit catalog variant / equipment | Main targets | Pattern / breadth | Load convention / concern |
|---|---|---|---|---|
| Squat | Barbell back squat; BB, rack, safeties | Quads, glutes, adductors | Squat / C | Total including bar; current name ambiguous |
| Front Squat | BB, rack, safeties | Quads, glutes | Squat / C | Total; moderate skill, weightlifting assistance |
| Goblet Squat | One DB OR kettlebell | Quads, glutes | Squat / C | Single implement |
| Leg Press | Specific leg-press machine | Quads, glutes | Squat-like / C | Machine identity and sled/display convention |
| Leg Extension | Leg-extension machine | Quads | Knee extension / I | Machine display; model-specific |
| Leg Curl | Seated OR lying curl as distinct variants | Hamstrings | Knee flexion / I | Machine display; posture ambiguous |
| Deadlift | Conventional BB from floor | Glutes, hamstrings, back extensors | Hinge / C | Total; sumo/trap-bar separate |
| Romanian Deadlift | BB variant; DB variant separate | Hamstrings, glutes | Hinge / C | Total vs per DB must be explicit |
| Hip Thrust | BB, padded support/bench | Glutes | Hip extension / C | Total; machine/bodyweight separate |
| Lunge | Bodyweight, DB or BB as separate variants | Quads, glutes | Lunge / C | Load/side/alternation ambiguous |
| Bulgarian Split Squat | Rear-foot support; DB variant separate | Quads, glutes | Split squat / C | Per DB; bodyweight variant separate |
| Calf Raise | Standing/seated/bodyweight distinct variants | Calves | Plantar flexion / I | Posture/equipment/load ambiguous |
| Bench Press | Flat bench, BB, rack, safeties | Pecs, triceps, anterior delts | Horizontal push / C | Total including bar |
| Incline Bench Press | Adjustable bench, BB, rack, safeties | Pecs, anterior delts, triceps | Inclined push / C | Total; store bench angle |
| Dumbbell Bench Press | Flat bench, DB pair | Pecs, triceps, anterior delts | Horizontal push / C | Per hand; old convention unconfirmed |
| Chest Fly | Flat DB fly; cable/machine separate | Pecs | Horizontal adduction / I | Per DB vs stack/display ambiguous |
| Push-up | Floor space | Pecs, triceps, anterior delts | Horizontal push / C | Reps-only; calisthenics; added load separate |
| Overhead Press | Standing BB strict press | Delts, triceps | Vertical push / C | Total; push press separate |
| Dumbbell Shoulder Press | DB pair; seated/standing separate | Delts, triceps | Vertical push / C | Per hand; support matters |
| Lateral Raise | DB pair; cable variant separate | Lateral delts | Shoulder abduction / I | Per hand |
| Pull-up | Pull-up bar, overhand | Lats, elbow flexors | Vertical pull / C | Reps-only; weighted/assisted separate |
| Chin-up | Pull-up bar, underhand | Lats, biceps | Vertical pull / C | Reps-only; weighted/assisted separate |
| Lat Pulldown | High pulley, bar, seated thigh support | Lats, elbow flexors | Vertical pull / C | Machine display, station identity |
| Barbell Row | BB; bent-over variant | Upper back, lats, elbow flexors | Horizontal pull / C | Total; unsupported trunk |
| Dumbbell Row | One DB, bench support variant | Lats, upper back, elbow flexors | Horizontal pull / C | Single implement, reps per side |
| Seated Cable Row | Low pulley, handle, seat/foot support | Upper back, lats, elbow flexors | Horizontal pull / C | Station display |
| Face Pull | Cable, rope; band variant separate | Rear delts, upper back, external rotators | Pull/external rotation / C | Technique and station-specific |
| Biceps Curl | DB supinated curl; BB/cable separate | Biceps | Elbow flexion / I | Equipment/per-hand ambiguity |
| Hammer Curl | DB pair, neutral grip | Brachialis, brachioradialis, biceps | Elbow flexion / I | Per hand; alternating reps explicit |
| Triceps Pushdown | High pulley, rope/bar attachment | Triceps | Elbow extension / I | Station display; attachment recorded |
| Dip | Parallel dip bars | Triceps, pecs, anterior delts | Downward push / C | Calisthenics; added/assisted separate |

Existing list gaps: trunk/anti-rotation, carries, locomotion, power, Olympic lifts, conditioning and mobility. Proposed staged additions (no automatic seed expansion in this task):

| Family | Representative variants | Metadata / generation boundary |
|---|---|---|
| Trunk | Plank, side plank, dead bug, Pallof press | Timed holds vs reps; anti-extension/anti-lateral-flexion/anti-rotation |
| Carry | Farmer carry, suitcase carry | DB/KB plus clear path; load per implement + distance/time |
| Simple home strength | Bodyweight squat, glute bridge, step-up, band row | Explicit box/anchor needs; distinct from loaded versions |
| Power | Kettlebell swing, box jump, medicine-ball throw | Ballistic hinge/jump/throw; skill and space requirements |
| Olympic | Snatch, clean & jerk; power clean/hang clean/push jerk derivatives | Advanced skill; opt-in only; no generic max transfer |
| Conditioning | Cycle, row ergometer, running, jump rope | Continuous or interval-compatible; time/distance/intensity needed |
| Mobility | Ankle mobility, shoulder mobility variants | Range/time recording; excluded from strength-load estimates |

## Sorting and selection behavior

Search name/aliases first. Filter chips: available equipment, muscles/body region, movement coverage, skill, discipline/purpose. Within one facet selected values use OR; across facets use AND. For an equipment profile, require one complete equipment group rather than matching any single item. Unknown metadata has an explicit Unclassified filter and is excluded from constrained automatic selection, while remaining available for manual logging.

Sort by name, equipment group, primary body region, coverage or recently performed; use stable ID as final tie-breaker. Multi-target exercises can appear in multiple grouped sections but are deduplicated in an actual generated workout. A muscle filter defaults to principal targets; optionally include secondary muscles. Show a result count and Clear filters. Keep detailed facets in one filter sheet to avoid crowding the ordinary exercise list.

Future planner: hard-filter equipment, recording capability and opted-in skill; choose goal-appropriate movement slots; rank coverage, preferences and familiar exercises; avoid repeated near-identical patterns; explain each choice. Different disciplines can coexist in a workout. No invented optimal weekly muscle score or automatic rehabilitation decisions.

## Starting-weight scaling plan

Separate **substitution suitability** from **load transferability**. Shared muscles or a movement pattern can identify alternatives but cannot supply a defensible numeric ratio.

Evidence hierarchy:

1. Same variant, same equipment identity/convention: show a recent performed working set with date/reps/effort. It is usually more useful than an all-time PR. Ask for target reps and effort before a numerical prescription.
2. Same variant with credible max/near-limit history: allow an explicitly estimated working-load preview; do not treat every logged set as a maximum. Existing `StrengthEstimates.epley` only uses 5/10-rep chart inputs and does not establish effort. Keep it unchanged; add a separate planner estimator later.
3. New variant with the user's explicitly entered or personally calibrated relationship: experimental editable trial-load estimate, with assumptions visible. After target observations exist, prefer them over transfer.
4. New variant with only a population study: show the study context as an optional reference; no default numeric autofill. Review each directed exercise pair independently before enabling any numeric model.
5. No valid relationship: leave weight blank and offer an equipment-appropriate familiarization entry. No generic bench-to-curl, squat-to-leg-press or deadlift-to-clean ratio.

Proposed directed relationship record: sourceVariantId, targetVariantId, source/target equipment constraints, normalized load convention, coefficient/range, evidence type, population/sample, source reference, review/version and exclusion reasons. Missing means unknown, never zero. Direction is explicit; reversing or chaining coefficients is not allowed by default.

For a future opt-in calibrated model:

`target capacity estimate = compatible source capacity × personal directed ratio`

`trial working load = target capacity estimate × target rep/effort factor × familiarization factor`

Then convert to the target entry convention and round **down to an actually available load**. Rep/effort and familiarization factors remain unvalidated product parameters; do not ship arbitrary defaults as scientific constants. Distinguish uncertainty in the source max, transfer ratio and rep/effort factor. If one is unknown, return no number. Qualitative evidence labels are not probability percentages or confidence intervals.

Illustration only: a 100 kg barbell max multiplied by the S5 group ratio 0.83 yields 83 kg combined DB capacity, or 41.5 kg per hand. This is not a starting weight. Applying hypothetical factors 0.65 and 0.80 would yield 21.58 kg per hand; if the equipment offers 20/22/24 kg, the arithmetic rounds to 20 kg. Those two factors are arbitrary demonstrations, and this calculation is disabled under the proposed default policy. Never label 83 kg or 41.5 kg as recommended working weight.

Personal calibration research path: once both exercises have several recent comparable observations, normalize convention and pair observations within similar dates/rep/effort conditions, inspect ratios and variability, and use a robust median only if consistency passes validation. A provisional minimum of three pairs is a product gate, not a validated sample size. Without effort/context, show an observed working-load relationship rather than calling it a max ratio. Validate with held-out future target sessions before claiming accuracy; no cloud aggregation.

Hard exclusions: unidentified machines/pulleys, unknown per-hand/total convention, bands, assistance, bodyweight-only records, changed range/technique, unconfirmed old metadata, stale or conflicting inputs, and advanced Olympic/ballistic lifts. An all-time PR is an upper historical achievement, not current readiness. Age, gender and bodyweight do not justify universal transfer multipliers. Body mass is relevant only where an explicit bodyweight-load model has been reviewed; do not silently add body mass to every exercise.

Preview should say, for example: “Experimental trial estimate from your barbell bench history; dumbbell load shown per hand. Review before using.” Display source date/reps, assumptions, evidence, equipment and editable value; require a deliberate Use action. Never auto-start, silently progress or rewrite recorded sets.

## Offline storage and durable implementation slices

Current `Exercise` stores ID/name/normalizedName only; seeding uses names and a one-time marker. Introduce immutable catalog keys separate from user names and keep the existing Room IDs. Bundle a small versioned curated Kotlin catalog; persist exercise-to-variant association and user metadata overrides locally. Relationships and sources can also be bundled, requiring no network/dependency. Never link catalog identity using current names at runtime.

Historical names are ambiguous and users can rename entries. An additive migration must default existing associations/load semantics to unknown. Suggest mappings for explicit user confirmation; exact name alone is insufficient for historical load reinterpretation. Custom exercises can remain unclassified or have explicit metadata. New variant choices cannot alter old workout names or sets. Snapshot load semantics/equipment identity on future workout entries so later metadata edits cannot change PR meaning. Preserve all existing progress charts; planner eligibility is a separate rule.

Next complete slices:

1. Implement catalog value types plus all 31 proposed mappings as deterministic data; test stable IDs, required fields, valid sources, equipment AND/OR semantics and ambiguity exclusions.
2. Add durable associations/overrides and equipment profiles with additive migrations and populated schema-4 tests; preserve workouts/templates/history. Confirm the manual mapping flow.
3. Add exercise filters/grouping with unknown handling; manually check clear filters, empty results, renaming, rotation and dark mode.
4. Implement deterministic strength draft generation and same-exercise references; no transfer numbers yet.
5. Add opt-in personal calibration only after conventions/effort/context exist and tests establish expected behavior. Timed conditioning is a separate migration before HIIT generation.

Estimator tests: kg/lb equivalence, total/per-hand distinction, no assumption for existing logs, no reverse/chained transfer, assistance/machine mismatch exclusion, nonfinite/negative inputs rejected, stale/conflicting inputs produce no number, discrete load rounding (including below minimum -> no feasible load), no writes to history and explanations retain source/version. Hold-out evaluation should report sample sizes, median absolute error, overestimation frequency and variation by exercise pair rather than one marketing accuracy number.

## Sources and review status

Sources consulted 2026-10-07. Summaries are paraphrased; do not copy proprietary exercise photos or instructional text into the catalog. Each future quantitative edge must cite the particular evidence it uses.

- **S1 — Professional exercise reference:** [ACE exercise library](https://www.acefitness.org/resources/everyone/exercise-library/). Supports faceted browsing; not numerical load transfer. Per-variant anatomical review remains pending.
- **S2 — Professional educational guidance:** [ACSM HIIT brochure](https://www.acsm.org/docs/default-source/files-for-resource-library/high-intensity-interval-training.pdf). Supports separating exercise modality from interval structure.
- **S3 — Governing body:** [IWF: the two lifts](https://iwf.sport/weightlifting_/the-two-lifts/), [preparation exercises](https://iwf.sport/2018/03/14/exercises-prepare-olympic-lifts/). Supports competition/derivative/assistance distinctions.
- **S4 — Peer-reviewed synthesis:** Nuzzo et al., Sports Medicine 54, 303–321; [author institution abstract](https://pure.solent.ac.uk/en/publications/maximal-number-of-repetitions-at-percentages-of-the-one-repetitio/), [DOI](https://doi.org/10.1007/s40279-023-01937-7). Supports exercise and individual variation in repetitions at relative loads; not cross-lift ratios.
- **S5 — Primary experiment:** Saeterbakken, van den Tillaar & Fimland (2011), Journal of Sports Sciences 29(5), 533–538; [author-uploaded paper](https://www.researchgate.net/publication/49746305_A_comparison_of_muscle_activity_and_1-RM_strength_of_three_chest-press_exercises_with_different_stability_requirements), [DOI](https://doi.org/10.1080/02640414.2010.543916). Twelve resistance-trained men; maximal chest-press conditions limit generalization to beginners, other exercises or submaximal sessions.
- **S6 — Older position stand:** [ACSM progression models (2009)](https://pubmed.ncbi.nlm.nih.gov/19204579/). Background on performance-based progression; not represented as the latest comprehensive guidance or a cross-exercise starting-load algorithm.

Verification: existing seed/model/estimation/design files reviewed, all 31 names covered, source claims checked against accessible source material. No build/test run is needed for this documentation-only slice. Numeric policy and UI remain proposals, requiring implementation and appropriate automated/manual validation.
