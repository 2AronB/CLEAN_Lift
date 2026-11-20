# Offline workout generator: data and preset plan

Implementation update 2026-10-07: [working generator](WORKOUT-GENERATOR.md) now supplies editable strength drafts, kettlebell straight sets, staple bias, remembered equipment selection, history-derived sessions and durable targets. Named profiles, timed presets and adaptive weight progression remain design proposals; follow that implementation note for current behavior.

Design checkpoint: 2026-10-07. Requested scope is planning; no generator UI, database migration or prescription logic has been implemented. Companion notes: [equipment planning](EQUIPMENT-PLANNER-DESIGN.md) and [exercise survey/load transfer](EXERCISE-CATALOG-RESEARCH.md).

## Product shape

Build a deterministic, editable draft generator using a small library of reviewed session structures. Its job is to choose sensible exercises and targets under explicit constraints, explain its choices, and save a new template after review. An offline rule engine is sufficient; no model service, account, dependency or Internet permission is needed.

Keep these dimensions independent internally, while offering familiar presets in the interface:

| Dimension | Choices | Meaning |
|---|---|---|
| Split / rotation | Full body A/B, pull–push–legs, later upper/lower | Which movement groups belong in each session |
| Equipment style | Available profile, bodyweight, dumbbells, gym | Which variants can be selected |
| Goal | General strength, muscle growth, power | What the prescription emphasizes |
| Set scheme | Standard straight sets, 5×5 main lifts, later intervals | Sets/reps/rest/effort, separate from exercise choice |
| Next-session choice | Infer from linked completed history, with manual override | No calendar or required weekly schedule |

Thus “bodyweight full body” is valid, and “5×5” is a prescription overlay rather than an independent body-part split. Do not offer every combination: use an explicit compatibility table. Power plus automatic 5×5 or timed HIIT plus ordinary strength logging should be rejected with a useful explanation.

## Small preset library

These structures are editorial starting templates, not proven superior sequences. Named slots describe roles, not compulsory exercises. Exact sets/reps/rest require a reviewed prescription version and user-editable targets.

| Preset | Ordered required slots | Optional slots | Initial boundary |
|---|---|---|---|
| Full body A/B | Priority lower-body pattern → push → pull → complementary lower-body pattern | Trunk; one accessory | Alternate squat/hinge emphasis and horizontal/vertical upper-body patterns across A/B; manage total workload rather than making both lower-body slots equally demanding |
| Pull | Priority row OR vertical pull → complementary pull | Rear-deltoid/upper-back accessory; elbow flexion | Deadlift is not silently required; hinge belongs to Legs by default |
| Push | Priority horizontal OR vertical press → complementary press | Lateral raise; triceps accessory | User can prioritize shoulders or chest; avoids three near-identical presses |
| Legs | Priority squat OR hinge → complementary lower-body pattern | Unilateral pattern; calf; trunk | Heavy hinge assigned here by default; preserve assignment across the rotation |
| Bodyweight full body | Squat/lunge → push-up progression → actual pull variant → hip-extension pattern | Rep-based trunk work | A pull-up bar/secure suspension or other reviewed support may still be necessary; floor-only means an explicitly incomplete pulling slot |
| Full body 5×5 | Selected principal squat/press/row-type slots | Individually prescribed hinge/trunk/accessories | Five working sets of five only on designated main lifts; do not impose 5×5 on every exercise or claim to implement a named commercial program |
| Explosive / power | Familiar opted-in power movement(s) → strength support → accessory | Trunk | Later phase: low-fatigue intent, skill/space checks and explicit effort/rest targets; Olympic derivatives remain individually opted in |
| Upper/lower | Upper: push+pull; Lower: squat+hinge | Appropriate accessories | Useful later addition using existing slots; keep initial preset picker small |

Within a session, prioritize the user's main goal, then demanding compound work, then accessories. Power/technical practice belongs early after appropriate preparation as an editorial rule subject to review. This is different from the rotation order “pull–push–legs.” Allow changing rotation order without redefining the session contents.

Example structures, with no implied working weights: Full body A could use back squat → bench press → supported DB row → lighter hip-extension accessory. Full body B could use a hinge emphasis → overhead press → pulldown → lighter split squat. These require matching equipment and workload review; they are examples of slots, not personalized training advice.

## Useful data, priority and missing-value behavior

### Required to produce a meaningful first draft

| Data | Source / ownership | Generator decision | Missing behavior |
|---|---|---|---|
| Preset and session choice | User chooses style; infer next session from clearly linked completed history | Required slot list and order | Offer a visible default/manual choice if history is absent or ambiguous; never guess a split from names alone |
| Equipment availability | Local equipment profile | Eligibility, substitutes, actual load increments | No location-based assumptions; allow explicit bodyweight floor-only profile |
| Session budget | User: exercise limit first, optional time budget | Accessory count and workload cap | Simple reviewed preset default shown and editable |
| Exercise variant metadata | Bundled catalog + confirmed overrides | Pattern, muscles, skill, equipment and recording compatibility | Unclassified items remain manually usable but are not automatically selected |
| Experience with selected exercises | User-confirmed familiar variants; history can support familiarity suggestions | Prefer known variants; gate technical movements | Basic variants only; logged once does not prove technique proficiency |
| Excluded / pinned exercises | User | Hard exclusions and required selections | Empty lists allowed; conflicting pins return an explanation |
| Set/repetition prescription | Versioned preset rules + user edits | Actual targets saved in template | Cannot call it 5×5 until rep targets persist |

Minimum screen: **Workout style**, **Equipment**, **Length**, **Effort today (1–5)**, then Generate. Show an inferred next session such as “Push — your last completed session was Pull,” with a change action. Full body similarly alternates A/B. Advanced options expose priorities, avoid/include lists, scheme and progression mode. No calendar setup is required.

### Valuable optional inputs

| Data | Why useful | Limits |
|---|---|---|
| Planned sessions per week / sequence | Distribute slots across a rotation; compare planned coverage | Not necessary for one-off draft; do not guarantee twice-weekly coverage from a three-day PPL cycle |
| Favorite exercises, preferred equipment, wanted novelty | Rank eligible candidates | Novelty defaults low; never replace all familiar lifts merely for variety |
| Current priority lift/body region | Order and optional accessory selection | Remains subordinate to exclusions/equipment/skill |
| Recent completed workouts with set counts and dates | Reference loads; show recent overlapping work | Logged work does not measure physiological recovery |
| Target and actual reps, warm-up/work-set flag, effort/RIR or RPE | Distinguish intent from completion; later progression | Current history lacks reliable work-set classification and target comparison; unknown is not failure |
| User says “lighter today” or skips a movement | Adjust draft volume/selection explicitly | No readiness score inferred from age, sleep guesses or PR age |
| Available increments / machine identity / load convention | Feasible load references and later estimates | Missing convention blocks numeric reuse, not exercise selection |
| Accepted substitutions and edited drafts | Remember explicit preferences | Save a preference only on deliberate user choice; no hidden behavioral profiling |
| Per-side convention and unilateral handling | Correct reps and duration estimate | Never double logged reps or load silently |

Keep medical diagnoses, injury inference, wearables, heart-rate collection, sleep tracking, body-measurement trends, calorie targets and demographic multipliers out of the initial generator. Existing profile gender/age and measurement screens do not create validated training/load formulas. A user-maintained Avoid list is enough for explicit exercise constraints without building a rehabilitation system.

### Catalog fields beyond browsing

Add exercise role eligibility (main/secondary/accessory/power), substitution family, supported prescription modes, progression/regression variant links, skill requirement, space/impact constraints, support/rack requirements and explicit load semantics. A progression link is an ordered variant relationship, not a strength conversion coefficient. Store broad editorial overlap flags (e.g., unsupported hinge/row demands on trunk/grip), not fabricated fatigue points or precise muscle stimulus units.

Every rule needs a source or an **editorial/product heuristic** label, version and short rationale. Scientific guidance informs broad choices; it does not prove our exact scoring weights, preset order or time estimates.

## Generator pipeline and conflicts

1. Normalize request, retain explicit unknowns, and load a snapshot of relevant local data.
2. Choose preset/session version. Validate split/style/scheme compatibility.
3. Apply hard constraints: equipment group satisfied, supported recording mode, exclusions, skill opt-ins and locked exercise eligibility. User pins do not silently waive constraints.
4. Fill required slots from a small curated candidate list. Rank lexicographically: explicit preference → confirmed familiarity → coverage/complementarity → less duplication → lower setup cost → stable catalog key. Avoid opaque weighted fitness scores. A small bounded search over slot alternatives is acceptable to avoid greedy dead ends; no elaborate optimizer is needed.
5. Add optional slots only within exercise/set/time budgets. A session-wide work-set cap and per-preset main-lift count are reviewed product limits, not universal medical thresholds.
6. Apply a versioned prescription by role and goal. Separate warm-up from working sets. Set and rep goals are targets, never fabricated performed sets. All accessories have their own scheme.
7. Estimate time transparently: setup/transitions + warm-up allowance + execution estimates + between-set rest. Unilateral side work and equipment changes matter. Show an approximate range; shorten by removing accessories before required patterns. If required work exceeds budget, return a conflict rather than secretly compressing rest.
8. Attach recent same-variant compatible references only. New-exercise weight transfer follows the separate conservative plan. Unknown load stays blank. No automatic increase in this phase.
9. Return ordered rows, reasons, gaps and input/rule versions. Generate again with unchanged inputs produces the same result; intentional alternatives use an explicit variant index, not invisible randomness.
10. User reviews, substitutes/reorders/edits, then saves a new independent template. Save never advances a rotation, overwrites another template or modifies workouts/history.

Partial drafts are allowed but must say what is missing: “No pulling exercise fits floor-only equipment,” not “complete full body.” Do not replace a pull with another push and count the slot satisfied. Locked conflicting choices show the specific conflict. Changing equipment after saving cannot rewrite the saved draft/template.

## Rotation and historical context

Preferred direction after user feedback: generate the next workout from completed history, without a calendar, deadlines or required days-per-week input. Persist the selected style/order and link generated workouts to a stable style/session key. Pull → Push → Legs → Pull and Full body A → B → A can be computed from the most recent eligible completed session in that style. Do not identify session keys from editable template names. Selecting a different style uses that style's own history. With no linked history, show an editable first-session default. An unrelated manual workout may inform recent-pattern context but does not silently advance the selected style; allow explicit classification.

Preview/save/start/cancel never count as a completed session. Derive from completion time plus stable ID tie-breaker, rather than start time alone. Keep pending/cancelled workouts excluded. Show which session informed the choice. If the referenced history is edited/deleted, recompute transparently and show the changed next-session reason; manual session choice always wins for that draft. No separate calendar cursor is needed initially. A persistent Skip/Advance action can be added later if useful; otherwise choosing the desired next session suffices. Existing history/old templates with no session keys remain unknown until explicitly classified.

Planned coverage and logged coverage are separate displays. Count working sets by primary targets and show overlap qualitatively; don't assign precise fractional stimulus credit to secondary muscles. Old sets lacking warm-up tags cannot support exact effective-volume totals. Time since training is context, not a declaration of recovery or mandatory rest clearance.

## Progression from suggested versus completed work

Progression is per exercise variant/equipment convention, not a global multiplier applied to a whole workout. The next Push can use the previous comparable Push even if Pull or Legs was performed more recently. Retain stable principal lifts so changing every exercise does not destroy useful comparisons. Store the accepted prescription at workout start; later draft edits and rule updates cannot change what was originally targeted.

Useful additional records: generated style/session key, accepted prescription version, per-working-set target load/reps or rep range, planned work-set count, actual work sets, optional effort feedback, substitutions and an optional reason for stopping early (time, difficult, discomfort, other). Compare actual performance against **accepted** targets, not an earlier suggestion the user rejected. Separate warm-ups. Don't infer failure from missing sets, extra sets, cancelled workouts or unknown effort. A substitution uses its own history; it cannot inherit the replaced lift's success or weight.

First adaptive policy proposal (editorial, needs tests and validation):

| Observed comparable outcome | Next suggestion |
|---|---|
| Required work sets completed below the top of the rep range | Hold load; suggest a small attainable rep increase within the range |
| Top of rep range achieved across required work sets, with acceptable effort on two comparable completed exposures | Offer the smallest feasible load increase, reset reps within range; show reasoning |
| 5×5 completed on two comparable exposures with acceptable effort | Offer the smallest feasible reviewed increment; maintain 5×5 |
| Missed target and user reports excessive difficulty | Hold/reduce next target after review; do not compensate by adding sets |
| Cut short for time, substituted, unknown convention/effort or inconsistent data | No automatic increase; show last compatible performance |
| New variant or long gap | Review/familiarization reference; no automatic max-derived progression |

Two exposures is a conservative product gate, not a scientific constant. Equipment increments can be disproportionately large; a reviewed relative-increase cap must also be met, otherwise keep load and use reps within the scheme or offer manual adjustment. No arbitrary fallback increment. Adjust one variable at a time: reps or load initially; sets and rest remain stable. Bound adaptation and show every proposed change. Never prescribe a failed-set retry loop or automatic PR attempt. More aggressive training does not bypass evidence, skill or equipment constraints.

Generation input **Progression: Off / Adaptive** is clearer than disguising progression as difficulty. Off means no performance-based increase; the user can still choose lighter work or edit a draft. Adaptive enables the bounded per-exercise proposal above. It does not guarantee an increase every session.

## Effort today: a possible 1–5 control

Use a session input labelled **Effort today**, with visible verbal anchors, rather than an unexplained “Difficulty.” Store requested session effort separately from reported post-workout effort and set-level RPE/RIR. It is neither a skill rating nor a promise that weight increases by a fixed percentage.

| Level | Label | Proposed effect |
|---|---|---|
| 1 | Very light | Recovery/practice draft; fewer optional work sets, no progression increase |
| 2 | Light | Reduced workload, conservative targets; no progression increase |
| 3 | Normal | Standard reviewed prescription; eligible adaptive progression |
| 4 | Challenging | More demanding approved effort target within the same constraints; eligible increase only if history supports it |
| 5 | Very challenging | Highest supported effort target; no automatic failure/max testing, added skill demand or larger load jump |

Default 3; remember a default only on explicit preference. Exact set changes, effort targets and any RIR mapping remain to be validated by goal/role; never multiply weights by 1–5 or increase sets, load and intensity together. 5×5 remains 5×5 on designated lifts: a lighter day changes its reviewed load/optional work or explicitly changes the scheme, never silently becomes 3×5. Power effort cannot be implemented by grinding to failure or adding fatigue. The first implementation can use only Light/Normal/Challenging if five independently meaningful levels would add false precision.

Optional completion question: “How did that feel? Easier than expected / About right / Too hard,” plus an optional stop reason. Treat this as coarse feedback, not an exact capacity measurement. Set-level RIR can provide more context later but prediction accuracy is imperfect; [the 2024 RIR scoping review](https://pubmed.ncbi.nlm.nih.gov/38563729/) discusses its usefulness and limits. [ACSM 2026 guidance](https://acsm.org/resistance-training-guidelines-update-2026/) supports individualization without requiring training to failure. These sources do not validate our five-level interface or two-exposure rule.

## Exercise library status

Implementation update 2026-10-07: the bundled version-1 catalog now defines 45 variants and deterministic metadata/filter/modifier helpers; see [catalog ownership and invariants](EXERCISE-CATALOG.md). The historical status paragraph below describes the earlier planning checkpoint. Durable associations, equipment profiles, UI and generation are still pending; existing seeds/history are unchanged.

Verified 2026-10-07: `CommonExercises.kt` seeds 31 names. `Exercise` holds only ID, name and normalized name. The companion survey maps the 31 names conceptually, but no stable variant catalog, machine/load semantics, equipment requirements, muscle/pattern tags, bodyweight progression library or power library has been implemented. It is not yet a generation-ready library. Next coding slice remains the curated catalog and meaningful metadata tests, followed by durable associations/equipment profiles. Start with the variants needed for Full body/Pull/Push/Legs, then add bodyweight gaps; power follows later. Do not tell the user that documentation mappings are working app metadata.

## Data model and existing-app integration

Keep curated catalog/preset/rule definitions bundled and versioned. Persist only user-owned profiles, preferences, confirmed mappings, templates and later rotation state. Suggested conceptual objects:

- `GeneratorRequest`: preset/inferred-or-overridden session, goal/scheme, profile, budget, requested effort, progression mode, include/exclude, skill opt-ins and relevant completed history.
- `PresetDefinition`: stable key/version, compatible styles/schemes, ordered required/optional slots, role-based targets, source/heuristic references.
- `GeneratedDraft`: ordered variant IDs and existing exercise IDs, target sets/reps/rest/effort, nullable reference load, explanations/gaps, input/rule versions.
- `PlannerPreferences`: explicit defaults/favorites/exclusions/confirmed familiarity. No copy of the entire workout history.
- Session provenance: stable style/session keys and completed-workout links; derive the next session from history without a calendar or separate cursor initially.

Current `TemplateRepository.save` accepts exercise IDs and optional set count only. Target reps/ranges, rest, effort, reference load and recording type are not durable template prescriptions. Add nullable target fields in a tested additive migration before claiming those schemes work. Keep recorded `WorkoutSet.reps` separate from target reps. Snapshot prescription and load semantics when starting a workout. Existing templates retain their prior behavior with null targets. Existing `GuidedPlan` uses set counts to guide completion; don't turn missed rep targets into inability to finish a logged workout.

A strength draft with order/set counts can ship earlier under an accurate label. 5×5 requires durable rep targets; bodyweight needs explicit rep-only semantics; timed holds/carries/HIIT need appropriate recording support. Power should follow curated power variants, skill opt-ins and preserved rest/intent targets. Lack of a rest timer does not prevent storing/showing rest targets.

## Evidence and preset policy

- [ACSM 2026 guidance summary](https://acsm.org/resistance-training-guidelines-update-2026/?nocache=1775184380) emphasizes consistency, individualization, training major muscle groups and different emphases for strength, muscle growth and power. It also supports useful training with bodyweight/bands. Consulted 2026-10-07; full position-stand link was inaccessible in this research session, so exact algorithmic prescriptions are not claimed from it. This updates the older 2009 background in the survey.
- [Ramos-Campo et al. (2024) systematic review](https://pubmed.ncbi.nlm.nih.gov/38595233/): 14 studies/392 participants found similar strength and hypertrophy results between split and full-body routines when volume was equated. Supports choosing structure by preference/schedule; does not specifically validate our PPL slot list or 5×5.
- [ACSM 2009 progression stand](https://pubmed.ncbi.nlm.nih.gov/19204579/) provides older sequencing background. Priority-first/demanding-before-accessory ordering is a starting convention, not a proven optimum for every goal.
- HIIT and Olympic definitions plus load-estimation limitations are sourced in [the companion survey](EXERCISE-CATALOG-RESEARCH.md).

5×5 is offered because it is a recognizable user-selected scheme, not because five sets are uniquely optimal. Do not treat moderate-load power guidance as permission to derive Olympic starting loads from squat/deadlift PRs. Exact preset volumes, recovery cutoffs, time constants, ranking choices and compatibility policies remain reviewed editorial decisions.

## Implementation order and acceptance cases

1. Catalog metadata plus equipment profiles and their database/domain tests.
2. Pure draft generator for Full body and individual Pull/Push/Legs sessions; order/set counts, explicit gaps, same-exercise references, editable new-template save.
3. Durable prescription targets; bodyweight variants and 5×5 on designated main lifts. Preserve legacy templates/history in populated migration tests.
4. History-derived next-session selection and per-exercise suggested-versus-completed progression with effort input; then power after skill/recording support. Upper/lower can reuse slots. Timed HIIT stays a separate later feature.

Required deterministic tests: complete equipment alternatives; pins vs exclusions; unknown/custom metadata; floor-only pulling gap; no duplicate exercise or unsupported mode; complementary full-body slots; hinge placement in PPL; only designated lifts receive 5×5; equipment-load limits; unchanged-input determinism; immutable history/templates; stale/incompatible loads blank; time-budget conflict; cancel/preview/save do not advance; completed-history session choice, ties, deletion and manual override; old template targets stay null after migration. Progression tests must cover accepted-vs-rejected targets, comparable exposures, time-shortened sessions, missing effort, substitutions, excessive equipment increments, per-hand/unit conversion, Off mode and levels 1/2 suppressing increases. Level 5 must never bypass a constraint or inflate the increment.

Manual acceptance once implemented: Generate a full-body draft with a Home DB profile; replace one exercise and save; reopen and verify order and targets. Generate floor-only bodyweight and verify the pulling gap. Generate 5×5 and check accessories retain their distinct targets. Cancel a draft and confirm existing templates/history remain intact. Test dark mode, rotation and filter clearing. No new emulator actions are needed for this planning-only slice.

Verification for this slice: reviewed catalog/equipment research and current template/guided-plan code; checked cited guidance and research abstract; documentation references checked. No code changes or build/test rerun.
