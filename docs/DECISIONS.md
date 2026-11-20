# Durable decisions

## D-011 — accepted 2026-10-16: proactive product engineering

Owner direction: “Start implementing changes. Update your AGENTS.md to not need explicit direction when my intent is clear” and build “the workout app android should have shipped with,” comprehensive, data-backed and free of ads/distractions/exploitation. This supersedes D-008's acceptance-only restriction. Select and implement coherent offline improvements using recorded intent, prioritizing common logging/reuse/review flows and progressive disclosure. Pending manual checks can be batched while independent safe work continues. Preserve privacy, durability, exact history and existing explicit dependency/SDK/permission/service/network approval boundaries. Do not repeatedly ask for authorization already recorded. Prepare a concrete proposal for a consequential ambiguity; routine UI/data-query choices are engineering defaults. No automatic coaching policy, networking or new permissions is inferred.

First slice: expandable complete previous-exercise set reference in Workout with explicit per-set copying. Choose the latest completed session containing saved sets by start time/ID and retain ordered repeated occurrences, exact original units/values, source positions and date. No automatic prescription or historical writes. See features/TEMPLATE-CONVENIENCE.md for acceptance/tests. Subsequent candidate: repeat workout/save history as template. Comprehensive scope is a direction, not a requirement to implement the entire candidate list at once.

Consolidated 2026-10-15 from prior instructions/checkpoints. These record existing decisions, not new approvals.

| ID | Decision and reason | Evidence |
|---|---|---|
| D-001 | Offline only, no accounts/monetization/tracking; dependencies/permissions/services need approval. Private local utility. | AGENTS.md and archived original instructions |
| D-002 | Kotlin/Compose/Material 3/Room, proportional architecture. Native interaction and understandable maintenance. Versions belong in build files. | app/build.gradle.kts; ARCHITECTURE.md |
| D-003 | Stable IDs, historical snapshots, preserving tested migrations. Later edits cannot silently rewrite records. | LOCAL-DATA.md; app/schemas/ |
| D-004 | Superseded by D-012 (four tabs, Settings behind a gear). Originally: Metrics / Templates / Workout / Widgets / Settings; Workout starts fresh launch. Separate capture, exploration, planning and tools. | 2026-10-07 checkpoints; PRODUCT.md |
| D-005 | Only documented Widgets→Metrics crossover. Explicit 2026-10-07 authorization includes shuttle/five benchmarks/AF and linked estimates/WHtR. | features/WIDGETS.md; archived CONTINUE |
| D-006 | Automated domain/database checks, manual UI/audio acceptance; no emulator UI driving. | AGENTS.md; feature checklists |
| D-007 | Android automatic backup remains disabled. The original local export/import deferral is superseded by D-009; other transfer/merge policies remain undecided. | AndroidManifest.xml; D-009; features/PORTABILITY.md |
| D-008 | Superseded by D-011: formerly acceptance or reported bugs only. | Latest pre-consolidation checkpoint; owner direction 2026-10-16 |

For future consequential decisions record: ID/date, proposed/accepted/superseded status, problem, chosen behavior, alternatives/tradeoff, user authorization evidence, affected features/tests and the decision superseded. Routine variable names do not need decision records. Preserve reasons when changing an earlier choice.

## D-009 — accepted/implemented 2026-10-15: explicit local portability

Authorization: owner requested implementation of the four suggestions and explicitly answered “Backup and restore.” A readable format-1/schema-8 JSON snapshot includes all source tables, sequences, profile and saved preferences, using the system document picker without new permissions/dependencies/networking. Restore replaces rather than merges, after isolated preflight, count preview and typing RESTORE. Unsupported versions and invalid files are rejected without source mutations. Audio files/grants and unsaved UI/timers are excluded; automatic Android backup stays disabled.

Chosen recovery: atomically stage settings and a data-generation ID in reserved app_metadata keys in the same transaction as source-row replacement. Apply preferences afterward and finish pending commits before startup ViewModels read preferences. This uses the existing metadata table without a schema migration; generation-scoped saved navigation prevents old editor state from attaching to restored rows. Direct live-database-file copies were avoided because WAL and separate preferences need consistent handling. Separate preference commits without a durable journal could leave mixed restored/current settings after interruption. No destructive Room fallback or app reset is involved. Contract/tests: features/PORTABILITY.md and BackupDataTest.

## D-010 — accepted/implemented 2026-10-15: Metrics entry convenience

Authorization: same four-suggestion request. Use existing Material date controls; preserve UTC calendar days. The daily form inserts only nonblank raw measurements in a single transaction, refusing existing metric/day conflicts rather than implicitly replacing stable records. Legacy exact-5/10-rep estimates and default-zero scenario move into chosen-lift details, retaining method labels and independent source history. No formulas or reference policy changed. Details: features/MEASUREMENTS.md, features/PROGRESS.md and features/METRICS.md.

## D-012 — accepted/implemented 2026-11-19: design review, navigation and palettes

Owner answers (2026-11-19): rebrand as a CLEAN app but plan the app-ID change rather than doing it blind; selectable palettes like Stretch; review plus deep implementation; consolidate tabs — "Design and go with it."

- **Tabs:** Workout / Plans / Progress / Tools; Settings leaves the bar for a gear icon on each tab root, opening full screen and returning to the origin tab. Supersedes D-004's five-tab layout and order. Reason: Settings is rare; four destinations give labels room at large text and keep supporting chrome compact (DESIGN-PREFERENCES). Workout moved first as the primary action. Alternatives: keep five tabs (more crowding), three tabs merging Tools into Progress (buries timers mid-workout). Internal enum/file/preference names unchanged, so saved state and backups are unaffected.
- **Palettes:** Iron (default), Chalk, Forest, Wallpaper (Android 12+, else Iron) plus System/Light/Dark mode, stored as `palette`/`theme` strings in `local-profile` so they travel with backups; validation accepts only known names; older backups without them restore to defaults. No schema change or dependency. A JVM test enforces 4.5:1 text contrast on every built-in palette.
- **Wording:** user-visible "template" becomes "plan"; Widgets becomes Tools; repeated instructions removed from Workout (stored notes text such as "Widgets • …" is unchanged).
- **Rest countdown:** visible-screen only, elapsedRealtime-based, started by saving a new set, using the plan's rest or 2:00; +30s/Skip; not stored; no sound, vibration, service or permission. Background alerts would need the approval boundary in AGENTS.md.
- **Rebrand:** planned here, executed 2026-11-20 as D-013.


## D-013 — accepted/implemented 2026-11-20: CLEAN Lift identity

Owner picked **CLEAN Lift** (2026-11-20). `applicationId` and `namespace` are now `dev.clean.lift`, the launcher name is "CLEAN Lift" and all Kotlin packages moved from `com.example.workoutlogger` to `dev.clean.lift`. The Room schema export folder moved to `app/schemas/dev.clean.lift.data.WorkoutDatabase` with identical JSON (schema files contain no package names), and asset paths in migration tests follow it.

Unchanged on purpose: database file `workout-logger.db`, schema version 8, preference file names and keys, and the backup `format` identifier `CLEAN-Workout-Logger` — so backups from the old app restore into CLEAN Lift and vice versa. Only the suggested backup file name changed (`CLEAN-Lift-<date>.json`). Internal names such as `WorkoutLoggerTheme` and `Theme.WorkoutLogger` stay as code names.

Consequence: Android treats CLEAN Lift as a different app. The old Workout Logger keeps its data until the owner uninstalls it; data moves only through Settings > Backup & restore. Alternative considered: change only `applicationId` and keep the `com.example` packages (less churn, but leaves a placeholder name the original setup flagged to finalize). The project folder moved to `CLEAN Development\CLEAN Lift` on 2026-11-20 (copied with Git history; the old `Desktop\CLEAN Apps` copy is retired).
