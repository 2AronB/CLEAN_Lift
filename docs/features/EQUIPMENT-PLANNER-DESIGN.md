# Equipment-aware workout planning: design before implementation

Implementation update 2026-10-07: [generator implementation](WORKOUT-GENERATOR.md) is now available under Templates, with remembered actual-equipment selection, catalog-backed drafts and durable targets. This note's named profiles/load inventories and advanced planning remain future work.

Authorized direction: offline, optional workout draft generation from selected equipment/environment and a generic goal, with historical-value references. Build other features first. This note is a design, not working generation UI.

Research extension (2026-10-07): see [exercise catalog survey and starting-load design](EXERCISE-CATALOG-RESEARCH.md) for the 31-exercise mapping, independent sorting facets, evidence sources and staged transfer policy. New-exercise loads remain blank by default without a validated relationship; muscle overlap alone cannot provide a coefficient.

Generator scope extension (2026-10-07): [preset/data plan](WORKOUT-GENERATOR-PLAN.md) defines Full body, Pull/Push/Legs, Bodyweight, 5×5 and later Power, required/optional inputs, deterministic slot selection, prescription persistence gaps and rotation semantics. Begin with single-session drafts; session sequences and numeric progression follow separate verified slices.

## Progressive path

Catalog foundation is now implemented: [structured catalog](EXERCISE-CATALOG.md) has 45 variants, equipment alternatives, muscle roles, mechanics and bounded modifiers. Persistent profiles/associations and generation below remain pending.

1. Named equipment profiles (Home, Garage, Gym, Hotel are editable suggestions, not equipment assumptions). User checks actual available items, ranges/increments where useful. Profiles should be reusable and editable offline.
2. Exercise capabilities tied to stable catalog IDs: movement patterns, required equipment alternatives, external-load convention, optional substitutions. Exercise names alone are insufficient. Preserve renamed IDs, support explicit metadata for custom lifts, and allow multiple capabilities rather than uncontrolled free-text tags.
3. Deterministic draft builder: user chooses goal (push/pull/legs/full body/bodyweight); filter equipment first; select reasonable distinct patterns from the curated list; show substitutions and reasons. Count/time controls keep the first version small. If no appropriate match, explain and return an editable partial draft; never invent equipment or exercises.
4. Review/save using existing template builder. Generation does not start a workout or overwrite a template. Reorder/remove/substitute before saving. No cloud model or network permission.
5. Reference previous loads from the same exercise ID and compatible load convention. Show date, unit, reps, effort information if available and what was reused. When unknown, leave load blank. Equipment ranges/increments constrain any proposed load. A workout goal does not imply a personal safe starting load.

## Decisions

- Home/hotel alone is not equipment availability. A hotel may have no gym or a fully equipped one.
- Bodyweight and externally loaded lifts must not share a naive load comparison. Dumbbell per-hand versus total, assisted pull-ups, cable stacks and different machines need explicit semantics before transferable estimates.
- A recent performed working set is a reference, not an automatic increase and not a 1RM load prescription. Longer gaps should show recency; optional conservative adjustment must be clearly labelled and user-editable.
- HIIT needs interval duration/rest and perhaps timed bodyweight sets. Current model logs weight/reps only. Do not pretend a strength template is a complete HIIT session; implement timed set support as its own tested migration first.
- Saved templates are independent snapshots. Equipment-profile changes cannot rewrite active workouts or history.
- One-action long-press overrides remain unchanged. Planner constraints are preview checks; unusual choices remain possible after an explicit warning/explanation, without creating a global unlock.

## First implementation scope for a following agent

Start with profile persistence + selection, then a small curated strength catalog mapping and deterministic tests. Add generation preview only after those pass. Required tests: missing equipment excluded, alternatives supported, deterministic output, no duplicate patterns unless requested, no-data loads blank, incompatible load conventions not reused, no template overwrite, profile deletion preserves prior data. Record migrations and green checkpoints in docs/CONTINUE.md.
