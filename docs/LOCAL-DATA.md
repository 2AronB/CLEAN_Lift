# Local data (schema version 8)

## Current schema summary

Schema 8 adds only `widget_attempts` through migration 7→8, with unique save token, protocol/day index and nullable `healthRecordId` foreign key using ON DELETE SET NULL. All previous workout/template/body/health tables are preserved. Exported schemas 1–8 are retained. Attempt and optional linked health-result saves are atomic/idempotent; original attempts are immutable. Deleting an attempt removes its linked health result if present; deleting a health result leaves its original attempt. See [Widgets](features/WIDGETS.md) for exact semantics and [CONTINUE](CONTINUE.md) for dated verification. Later sections describe earlier schema stages and remain historical implementation detail.

Migration 4→5 adds nullable catalog identity, generated-session provenance and rep/rest target columns plus a unique nullable catalog-key index, preserving all old records with null new metadata. Generated exercises/templates are inserted together transactionally; historical IDs are never remapped by names. Target reps/rest are snapshotted when starting a workout and remain separate from actual logged values. See features/WORKOUT-GENERATOR.md. Exported schemas 1–6 are retained. Migration 5→6 adds only nullable circuitRound columns to template_exercises/workout_exercises; existing rows default null and all prior fields are preserved. Circuit rounds snapshot into active/history entries. CatalogRepository installs all 66 rep-loggable catalog variants transactionally without remapping old exercise IDs. Gym/Home equipment is stored separately in private preferences, with the former single selection retained under Home.

Room stores all records in the app-private workout-logger.db file. Schema 1 was the first database. Migration 1 to 2 adds only app_metadata, a key/value table used to record one-time starter-exercise installation. Migration 2 to 3 adds workout_templates and template_exercises, plus nullable templateName/plannedSets and default-zero overrideCount/completed columns to workouts/workout_exercises. It alters existing tables in place and does not drop or rebuild them. Existing workouts default to ordinary empty-start behavior; names, IDs, weights, reps, notes, RPE and timestamps are preserved. Exported schemas 1, 2 and 3 are retained in app/schemas. Future version increases must include preserving migrations and tests. Destructive migration fallback is intentionally absent.

- exercises: stable generated ID, display name, unique normalized name. Leading/trailing and repeated whitespace are cleaned; duplicate checks ignore case using Locale.ROOT. Names accept 1–100 characters.
- workouts: start time and optional finish time in Unix milliseconds, plus notes. Null finish time represents an unfinished workout.
- workout_exercises: workout and catalog exercise IDs, ordered position, and the name snapshot used when logging. Renaming a catalog exercise preserves historical names. Repeated occurrences of the same exercise are allowed at different positions.
- workout_sets: parent entry, ordered position, integer weight in thousandths of the stated kg/lb unit, reps, optional RPE in tenths, and notes. For example, 185.125 lb is stored as 185125 and RPE 8.5 as 85.

Foreign keys reject orphaned entries/sets. Unique parent-position indexes prevent ambiguous ordering. Removing an exercise referenced in history is restricted; deletion is not exposed in the UI. Deleting a workout explicitly would cascade to only its own entries and sets. Inserting uses ABORT instead of REPLACE, avoiding accidental deletion of referenced records. Workout start, set writes, removals, finish, cancel, and common-exercise installation run in database transactions. Concurrent starts reuse the existing active session. Finished sessions reject active-workout logging mutations; only explicit history corrections may update their numeric set values. Removing a middle set leaves internal ordering gaps; new sets append after the current maximum rather than overwriting another row.

Room performs suspend operations off the main thread. Exercise list changes arrive through Flow. A lifecycle ViewModel owns loading and saving; draft text is saved across screen recreation. A failed save keeps the editor open.

Explicit local JSON backup and confirmed replacement restore are implemented; Android automatic backup remains disabled. See [portability](features/PORTABILITY.md) for compatibility, exclusion and recovery rules. App data survives closing, force stopping, and normal app upgrades. Uninstalling the app or clearing its storage removes the database. Do not use those actions for persistence testing.

## Verification

Local JVM tests cover name cleanup, length limits, case folding, and Turkish-locale stability.
Android database tests use a separate test database and cover reopen persistence, exact weights/RPE/notes/unit retention, historical name preservation, duplicate inserts and rename conflicts, orphan rejection, transaction rollback, set ordering, restricted exercise deletion, and cascading workout deletion. Tests never clear the user's production database.

Workout supports basic logging and a latest completed summary. Milestone 5 adds completed-history summaries and individually loaded workout details. The list initially loads 50 summaries and supports Show more, avoiding loading all historical sets for the list. History corrections update only a set's weight, unit, and reps in a transaction after verifying that the workout is finished and the set belongs to it. Names, notes, RPE, ordering, and timestamps remain intact. No schema migration or new dependency is needed for Milestone 5. Room/KSP, AndroidX lifecycle integration, and JUnit/AndroidX test dependencies are the only additions needed for these milestones; none provide networking or telemetry.

Milestone 4 tests additionally cover integer weight parsing/formatting, full-session reopen persistence, active-session recovery, concurrent starts, edit/remove ordering, finish validation, cancellation isolation, idempotent starter seeding, and migration of populated schema-1 records. No UI automation is used; screen acceptance checks are manual.

Milestone 5 tests cover completed-only history, stable newest-first ordering including tied finish times, limited summary queries and counts, correction persistence across database reopen, preservation of every other set/workout field, isolation from other sessions, and rejection of invalid or incorrectly owned corrections.

## Templates and guided sessions

workout_templates stores a cleaned display name and unique normalized name. template_exercises stores catalog exercise IDs, ordered positions, and nullable plannedSets (null = Until done, otherwise 1–99). Template save/replacement and deletion are transactional. Templates allow up to 100 exercise entries. Deleting a template cascades only its own rows. Repeated exercise entries are supported.

Starting a template is transactional and rejects an already-active session. It copies the template name, current catalog names, positions and planned counts into the workout, with no foreign key from the workout to the template. Editing or deleting a template therefore cannot alter that snapshot. completed marks exercises advanced past explicitly; it and the saved sets make guidance resumable after reopening.

Each plan deviation needs its own explicit override argument. Successful deviating writes increment overrideCount in the same transaction. This counter is an indication of deviations, not a blanket permission for subsequent writes. Normal saved-set corrections never change it. Invalid values and finishing without any saved set remain prohibited even with an override. Fixed-set append limits, plan order, explicit Until done completion, skip/finish rules, and adding/removing guided exercises are enforced by the repository as well as the UI. Empty-start workouts retain their previous finish validation.

Milestones 6–7 tests cover order/limits, Until done, one-action overrides, validation/rollback, template snapshots, restart continuation/override persistence, and populated version-2 migration plus version-1 chained migration. No UI automation is used.

## History/progress/body iteration

Migration 3 to 4 creates body_measurements and its unique metric/epochDay index only. Existing tables are not altered, dropped or rebuilt. Exported schemas 1–4 remain available. Populated migration tests preserve active/finished workout data, plan targets/completion/override counters, template IDs/rows, notes/RPE and seed metadata through all supported paths.

Body records store a predefined metric, calendar day, exact integer thousandths and entered unit. Mass kg/lb and circumference cm/in are validated separately. One entry per metric/day; conflicting inserts or edits abort rather than replace data. Repository transactions preserve prior records on validation/conflict failure. Left/right measurements remain separate. See features/MEASUREMENTS.md.

Progress uses a compact read-only projection of completed sets grouped by workout and stable exercise ID, converted to kg for comparison. No derived records are written to history. Correction invalidates the Room projection and updates progress. Break scenarios cannot modify stored data. See features/PROGRESS.md.

Prior values come only from completed workouts for the same exercise ID; the UI shows date/unit/reps and copies only on explicit action. A copied value is still an unsaved draft. Template duplication produces a draft with a null ID so saving creates a new independent template. See features/TEMPLATE-CONVENIENCE.md.

## Metrics snapshots (schema 7)

Migration 6→7 adds only `health_records` and its nonunique kind/calendar-day index. Existing schema-6 workout, set, template, catalog and body-measurement rows are untouched. Exported schemas 1–7 are retained. Populated migration tests cover every starting schema 1–6 through 7; the version-6 fixture checks notes, circuit rounds, template rows, catalog identity, measurements and metadata explicitly.

Each health result stores its method/kind, calendar day, computed or reported result, formula version, nullable canonical cm/kg inputs, age/profile-gender snapshot, time in seconds, distance in metres, finishing pulse and optional notes. Different body-fat and VO₂ methods use distinct kinds. Multiple results per method/day are permitted. The repository validates and recomputes calculated results transactionally; edits cannot change kind, and deletion affects only that record. Changing profile or original body measurements does not rewrite saved snapshots. Height is a new predefined body metric using the existing length storage; no table alteration is required.

Strength and Big 3 charts remain read-only projections from completed workout sets and stable exercise IDs. Rep records, estimates, same-day bodyweight ratios, same-session totals and separate-date personal-best sums are distinct. Correcting history refreshes these projections. Private metric-page preferences hold visibility/groups/order/units/reference/average choices, selected lift IDs and rep count, and Big 3 mappings. They add no dependency, permission or networking. The implemented local backup includes health snapshots and metric preferences alongside the other source data. See features/METRICS.md for ownership, formulas, evidence and manual acceptance.


## Convenience and portability invariants — 2026-10-15

Schema remains 8. Daily-measurement inserts run in one transaction, with the existing unique metric/day constraint and exact entered-unit thousandths; a conflict or invalid input rolls back the whole batch. No source records or health snapshots are derived/rewritten automatically.
Local backup serializes source tables logically from a read transaction, including active workouts and sqlite_sequence high-water marks; it does not copy a live database file or omit WAL records. Restore validates an isolated candidate, then replaces data and stages settings in one transaction with FK enforcement. Reserved app_metadata keys __backup_pending_settings_v1 and __backup_generation_v1 are internal recovery/navigation metadata and excluded from exported source rows. A pending settings marker survives process loss and is removed only after all preference commits succeed. Startup gates feature ViewModels until recovery finishes. This is an explicit user-confirmed replacement feature, never a debugging reset or migration fallback. Full policy: [PORTABILITY](features/PORTABILITY.md).