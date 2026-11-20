# Current continuation checkpoint

Read AGENTS.md and PRODUCT/ARCHITECTURE/DECISIONS/DEVELOPMENT, then the relevant feature contract.

## Current work — four requested conveniences, 2026-10-15

The user requested all four Metrics follow-ups and explicitly chose **backup and restore** (not export alone). Implement in verified slices: native dates, atomic daily measurements, chosen-lift estimate consolidation, local versioned backup/restore. No dependencies/permissions/services or Android automatic backup changes.

First verified slice: shared Material date picker replaces body/health entry and custom-range date typing. Calendar-day storage and 1970-through-today limits are preserved. Debug build, all 102 JVM tests and lint passed on 2026-10-15. No schema/repository changes in this slice. Manual calendar/rotation/large-text/phone checks pending.

Second verified slice: Metrics > Daily measurements saves nonblank raw entries in one transaction, rejects duplicates/invalid values without partial writes, and preserves exact units/IDs. Build, all 104 JVM tests, lint (0 errors/32 warnings) and all 44 isolated emulator database tests passed on 2026-10-15, including new batch rollback/reopen tests. Manual acceptance pending. Third verified slice: legacy positive-load 5/10-rep estimates and default-zero break scenario are expandable within each chosen-lift detail, above its recorded-value list. Removed the separate overview Lift estimates entry. Pure projection retains one point per workout/exercise, stable IDs and original 5/10-only semantics. Build, all 105 JVM tests and lint passed 2026-10-15; no new database changes since the 44-test run. Manual UI acceptance pending. Next: versioned local backup/restore per features/PORTABILITY.md. Backup/restore needs full-data/settings snapshot, preflight validation, replacement preview/confirmation and recovery of interrupted settings application before ViewModels start. No production reset/uninstall permitted. Schema remains 8. Desktop emulator-5554 reconnected without data changes and ran the isolated data suite; no UI automation.

Previous baseline and documentation consolidation: [archived checkpoint](archive/CONTINUE-before-convenience-2026-10-15.md). Historical 102-test and 42-database-test evidence from 2026-10-07 is not a current data-test run.

Verify with scripts/verify.ps1; add -DatabaseTests only for the desktop emulator when ready. Manual: Android Studio > Device Manager > start existing Medium Phone > select it in toolbar > Run. No UI automation; preserve device data.

No Git repository exists. Local Git checkpoints are recommended; no remote publication is implied. Next task remains the explicit user request above, not older backlog plans.
