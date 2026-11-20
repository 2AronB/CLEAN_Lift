# Structured exercise catalog

Implementation update 2026-10-07: catalog version 3 now has 69 variants, adding 16 bodyweight/pulling/core/hip/arm/kettlebell options to version 2. The 66 rep-loggable variants now install into Exercises and ordinary pickers through CatalogRepository, preserving existing mapped IDs and all old history. Time/distance variants remain outside the current logger. Generator schema 6 adds durable ab-circuit rounds; Gym/Home equipment preferences, max 10, compound-first ordering and honest floor upper-back fallback are implemented. Legacy/custom history stays unclassified. See [generator implementation](WORKOUT-GENERATOR.md). The original foundation checkpoint below describes the earlier version-1 slice.

Implemented 2026-10-07 in `data/ExerciseCatalog.kt`; domain tests in `ExerciseCatalogTest.kt`. Version 1 has 45 explicit variants: all 31 existing starter names have a proposed variant association, plus 14 bodyweight/trunk/carry/power variants. Catalog definitions are bundled, offline and independent from Room exercise IDs. No new seeds, migrations, dependencies or permissions. This slice provides domain data/helpers, not browsing UI or a working generator.

## Meaning and evidence

Muscle regions support broad filters; finer muscle groups support primary/secondary/stabilizer roles. “Primary” is direct movement emphasis; secondary and stabilizer roles do not satisfy primary coverage. These are curated biomechanical tags, not measured stimulus percentages, an exhaustive anatomical inventory or a guarantee that every muscle receives sufficient training. Coverage should be checked across chosen sessions against an explicit desired-target list, rather than adding isolation exercises for every tagged stabilizer. Some catalog groups (e.g. upper back/forearms) remain broad. Version 3 adds primary hip-abduction and rotator-cuff entries. Missing primary coverage remains visible rather than being filled from stabilizer involvement. Floor W/T raises use scapular retraction, not horizontal/vertical resisted pulling; they do not satisfy lat coverage. Their ACE reference supports the shoulder/back stabilization context, while rep-based raise targets are editorial adaptations of demonstrations that include holds.

Stability describes external support/balance context, not a safety score or measured instability. Explosive intent is deliberately allowed only for explicitly curated ballistic exercises in this version. STANDARD range means the variant's ordinary intended range, not a prescribed joint angle or forcing maximum range. PARTIAL modifiers are representable but should require explicit user choice; the generator must not infer an ideal depth. ISOMETRIC holds use TIME recording and stay excluded when only rep logging is supported.

Most rows use `editorial-v1` provenance: the survey and professional references informed the taxonomy, but these row-level muscle/skill/stability decisions have not been individually experimentally validated. No numerical load coefficients are provided. Specific supporting references:

- [ACE Romanian deadlift](https://www.acefitness.org/continuing-education/certified/may-2025/8865/the-ace-do-it-better-series-the-romanian-deadlift/) informs its posterior-chain tags; spinal extensors are represented as stabilizers rather than assumed dynamic movers.
- [IWF two lifts](https://iwf.sport/weightlifting_/the-two-lifts/) supports snatch/clean-and-jerk competition identities, not all muscle tags.
- [ACE library](https://www.acefitness.org/resources/everyone/exercise-library/) informs region/equipment browsing.
- [NSCA core development discussion](https://dxpprod.nsca.com/education/articles/kinetic-select/multi-joint-training-versus-isolated-training-for-core-development/) informs the distinction between supported and free-weight stabilization demands; does not validate our categorical stability scale.

## Modifiers and deterministic identity

Avoid combinatorial generation: curated immutable variants are selected first, then a bounded modifier record is validated. Current modifiers: range (standard/partial or variant-native isometric), intent, pause 0–5 seconds, optional eccentric 1–10 seconds. These bounds are product validation limits, not scientific optimums. Explosive variants reject tempo/pause/partial combinations; holds reject dynamic tempo. No modifiers silently alter muscle roles, equipment requirements or skill.

Use separate variant keys for equipment, grip, support, stance/laterality, incline or movement changes that affect mechanics/load comparison. Tempo/pause/partial range remain modifiers, but `comparisonKey` includes all modifiers so their historical loads are not automatically interchangeable. That key does not by itself permit machine/load reuse: future history must also snapshot equipment identity and load convention. Different variants may share a family for substitutions; family does not confer a weight ratio.

The catalog's `legacyCandidates` provides suggestions only. Ambiguous names such as Squat, Leg Curl and Lunge require confirmation; even explicit names do not establish historical load semantics. Renamed/custom entries are never automatically mapped. Existing history remains untouched. Per-hand/single-implement/total/machine/bodyweight semantics are explicit for future entries only. The load-reps recording enum describes capability; machine identity, per-side reps and actual available loads need later durable metadata.

## Helpers and boundaries

- `availableWith`: OR across equipment alternatives, AND within each group. Profiles must later handle counts, attachment compatibility, actual loads, safe support/space and platform capabilities. Presence alone does not validate an equipment setup.
- `eligible`: checks equipment, supplied recording modes, skill ceiling and explosive opt-in; stable-key sorted. The caller must supply genuinely supported recording modes; current app cannot log timed holds or carries. Skill approval should be exercise-specific in the later generator, beyond this broad ceiling.
- `missingPrimaryTargets`: returns missing requested muscle targets, rejects unknown keys, never credits secondary/stabilizer roles as direct work.
- `supports`/`comparisonKey`: validate modifiers and retain separate comparison identity. A ballistic variant requires its native BALLISTIC intent; default CONTROLLED is not valid for it.

Tests cover catalog integrity/legacy-name coverage, complete equipment alternatives, capability/skill gates, deterministic ordering, floor-only pulling gaps, direct muscle coverage and incompatible modifier comparisons. No database tests needed because no persistence changes occur.

Next slice: additive persistent exercise-to-variant association with explicit confirmation and user overrides, equipment profiles and snapshot semantics. Then filter UI and generator slot selection. Before claiming full anatomical coverage, add reviewed direct-target variants for desired missing groups; before Power UI, review skill/space and prescription behavior.
