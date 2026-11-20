# Current continuation checkpoint

Read root AGENTS.md, this file, then only the relevant feature note from [ARCHITECTURE.md](ARCHITECTURE.md). Product boundaries: [PRODUCT.md](PRODUCT.md). Change process: [DEVELOPMENT.md](DEVELOPMENT.md). Lasting decisions: [DECISIONS.md](DECISIONS.md).

## Current work — documentation consolidation, 2026-10-15

Created a reusable conversational requirements prompt, optional global preferences and per-project instruction starters under [development/](development/README.md). Replaced outdated root guidance with the current engineering contract. Consolidated the product baseline and decisions, extracted feature ownership into ARCHITECTURE and separated historical checkpoints from this entry point. Archived the former AGENTS, CONTINUE and ROADMAP before editing. Marked old setup/roadmap documents historical and corrected LOCAL-DATA's schema summary.

This is documentation-only work: no app code, schema, dependencies, permissions, services, build settings, global instructions or user data changed. No app build/tests were rerun; the older automated results below remain historical evidence. Document consistency/link checks are recorded in the completion response. The reconstructed baseline does not imply new scope approval or manual acceptance.

## Last verified application checkpoint — 2026-10-07 (not rerun today)

Metrics usability corrections: history is solely workout review/correction with visible list Back; duplicate measurement/progress menus removed. Existing lift estimates remain reachable through Metrics with chart-origin return. Body-metric Add/saved rows open the editor directly, new entries honor preferred units and corrections retain original units. Save/delete/discard returns to the original chart. Overview has saved category/recorded-only filters; comparisons resolve distinct visible metrics and explain zero-baseline limits. Widget input details expand on selection; custom dates have keyboard padding.

Historical verification: debug build, all 102 JVM tests and lint passed. Database tests were not repeated for that final UI/comparison slice because schema/queries/repositories/migrations did not change. Schema remains 8. No new dependencies/permissions; the merged manifest was checked for no INTERNET permission. Manual navigation/rotation/visual/phone checks remain pending in [METRICS.md](features/METRICS.md).

Last Widgets checkpoint: authorized calculators, general/tempo/PACER/HAMR timers, AF PFRA quick/guided/target modes and five benchmarks are implemented, with immutable local attempts and the documented Metrics crossover. The final Widgets build, 98 JVM tests and lint passed (0 errors/30 warnings). The immediately preceding full database/restoration run passed all 42 isolated emulator tests, including populated migrations 1–7→8. The later AF age guard/label changes did not change database/audio/timer behavior. UI/audio/phone acceptance remains pending in [WIDGETS.md](features/WIDGETS.md).

APK from the last build: app/build/outputs/apk/debug/app-debug.apk. Do not infer a new build or current device installation from its presence. Earlier milestones 0–5 were manually accepted; subsequent features are not automatically accepted.

## Next action

Await user acceptance feedback or a reported bug; prioritize the specific problem. Do not implement another feature from an old plan by default. The reusable kit is ready for starting a separate app in another workspace. Existing permissions for incremental offline work remain subject to AGENTS/PRODUCT boundaries; suggested backlog items are not next-task orders.

For manual app checks: Android Studio > open this project > View > Tool Windows > Device Manager > start the existing Medium Phone > choose it in the top toolbar > click Run. Follow the relevant feature checklist. Do not Wipe Data, uninstall or clear storage. Do not drive emulator UI automatically.

## Verification commands for future code changes

```powershell
.\scripts\verify.ps1
# If data/restoration behavior changes and the desktop emulator is ready:
.\scripts\verify.ps1 -DatabaseTests
```

The script configures Android Studio Java and existing caches. Check adb devices before connected tests; target only emulator-5554 by default, never the WiFi phone. Test databases must be isolated from workout-logger.db. Emulator availability has not been rechecked for this documentation task.

No Git repository exists as of 2026-10-15. Local Git checkpoints are recommended before future coding; no repository/remote was created here. Do not confuse source control with on-device data backup. Android backup remains disabled; export/import is deferred.

## Historical evidence

- [Former complete continuation log](archive/CONTINUE-2026-10-15.md): earlier verification, migration and authorization history; old “next” sections are superseded by this file.
- [Original project instructions](archive/AGENTS-2026-10-15.md).
- [Former roadmap](archive/ROADMAP-2026-10-15.md).
- Feature contracts and manual checklists remain under features/. Older plans are design history when the current feature contract reports implementation.
