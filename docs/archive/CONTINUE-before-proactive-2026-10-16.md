# Current continuation checkpoint

Read AGENTS.md, then PRODUCT/ARCHITECTURE/DECISIONS/DEVELOPMENT and the relevant feature contract. Archived checkpoints are historical evidence.

## Current verified application state — 2026-10-15

Completed the owner's four requested conveniences. They explicitly chose backup **and restore** when asked about recovery scope.

- Shared native Material calendars for body/health entry and Metrics custom ranges, retaining UTC calendar-day storage and 1970-through-local-today selection.
- Metrics > Daily measurements saves nonblank raw entries for a day together; conflicts/invalid input roll back all inserts, without replacing existing metric/day records. Exact units/IDs are preserved; no automatic health snapshots.
- Each chosen-lift detail has expandable exact-5/10-rep estimates and the default-zero break scenario above recorded values. Removed the separate Lift estimates destination; method labels, date filtering and chart-origin history corrections remain.
- Settings > Backup & restore creates readable format-1/schema-8 JSON and validates a selected file before count/profile preview and typing RESTORE for full replacement. All source tables/sequences/profile/preferences are included; unsaved drafts, running timers, audio files/grants are excluded. Atomic data replacement stages pending settings and a navigation generation in existing app_metadata; startup completes preference recovery before feature ViewModels start. Success restarts fresh navigation. Backup limits and compatibility are explicit in PORTABILITY.md.

Final verification actually run on 2026-10-15: scripts/verify.ps1 -DatabaseTests passed debug build, all 105 JVM tests, lint (0 errors/31 warnings), and all 50 isolated desktop-emulator database/restoration tests. New tests cover daily-batch validation/rollback/reopen; legacy estimate projection; backup all-table/sequence/settings round-trip/reopen; corruption/version/type/overflow/relationship rejection; transaction rollback and interrupted/partial-settings recovery. Existing populated migration chains remain green. Emulator-5554 reconnected without wiping; no UI automation was used. Tests use isolated databases and prefixed preferences, never production workout-logger.db/profile.

Schema remains 8, with no dependencies/permissions/services/toolchain changes. Existing migrations/exported schemas remain intact. Merged manifest has no INTERNET permission and automatic Android backup remains disabled. Lint warnings concern pinned toolchain/library updates, existing resources/plurals/launcher artwork and SharedPreferences KTX suggestions; no checks were suppressed.

APK: app/build/outputs/apk/debug/app-debug.apk. This is a newly built artifact; do not infer a manually launched/accepted device UI from the automated test installation.

## Next action

Manual acceptance or reported bugs only. See [Metrics](../features/METRICS.md), [measurements](../features/MEASUREMENTS.md), [progress](../features/PROGRESS.md), and [backup/restore](../features/PORTABILITY.md). Android Studio > open this project > View > Tool Windows > Device Manager > start existing Medium Phone > select it in toolbar > Run. Test daily entry/date selection/draft rotation, chosen-lift estimate-to-history return, then backup save/preview/cancel and an intentional emulator replacement round trip using a freshly saved backup. Only the owner's report completes visual/emulator/phone acceptance. No power-loss/UI automation acceptance is claimed.

No local Git repository exists; local Git checkpoints are recommended before future changes, with no remote publishing implied. Source rollback is not a database downgrade or device-data backup.

## Historical evidence

- [Previous baseline/documentation consolidation](CONTINUE-before-convenience-2026-10-15.md).
- [Intermediate verified convenience slices](CONTINUE-convenience-slices-2026-10-15.md).
- [Original complete application history](CONTINUE-2026-10-15.md).

Use scripts/verify.ps1 for app changes; add -DatabaseTests for data/restoration changes with emulator-5554 ready. Do not target the WiFi phone, wipe storage, uninstall or use destructive Room fallback. Feature ownership and lasting decisions remain in ARCHITECTURE.md and DECISIONS.md, rather than duplicated task lists here.

