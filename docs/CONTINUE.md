# Current continuation checkpoint

Read AGENTS.md, PRODUCT/ARCHITECTURE/DECISIONS/DEVELOPMENT and the relevant feature contract. Archived checkpoints are historical evidence.

## Standing authorization

Unchanged from 2026-10-16 (D-011): proactive offline improvements toward the workout app Android should have shipped with, within the dependency/permission/service/network approval boundaries. D-012 (2026-11-19): follow `CLEAN Development\DESIGN-PREFERENCES.md`; four-tab navigation and palettes accepted. D-013 (2026-11-20): the app is **CLEAN Lift**.

## Current change — 2026-11-20: CLEAN Lift identity (D-013)

- `applicationId` and `namespace` are `dev.clean.lift`; launcher name "CLEAN Lift"; all Kotlin packages moved from `com.example.workoutlogger` to `dev.clean.lift` (main, test, androidTest).
- Room schema export folder renamed to `app/schemas/dev.clean.lift.data.WorkoutDatabase` (JSON unchanged); migration-test asset paths updated.
- Unchanged: database file `workout-logger.db`, schema 8, preference files/keys, backup format `CLEAN-Workout-Logger` format 1. Backups from the old app restore into CLEAN Lift. Suggested backup file name is now `CLEAN-Lift-<date>.json`.
- Pre-rebrand snapshot (includes the 2026-11-19 design pass): `tmp/snapshot-2026-11-20-before-rebrand/`. Older pre-design snapshot: `tmp/snapshot-2026-11-19/`.

The 2026-11-19 design pass (four tabs, palettes, quieter screens, rest countdown) is in the [previous checkpoint](archive/CONTINUE-before-rebrand-2026-11-20.md) and [REVIEW-2026-11-19](REVIEW-2026-11-19.md); its manual checks are carried below.

Verification actually run 2026-11-20 after the rename: `scripts/verify.ps1 -DatabaseTests` passed debug build, 112 JVM tests, lint 0 errors / 32 warnings, and all 51 isolated database tests on emulator-5554, reported under `dev.clean.lift.*`. Merged manifest: package `dev.clean.lift`, no INTERNET permission; the only `uses-permission` is AndroidX's own `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` guard, renamed with the package. Run the script without redirecting output (`2>&1` or `*>`): PowerShell 5.1 then treats Gradle's JDK warning as an error.

## Owner steps to move to CLEAN Lift (phone)

The old Workout Logger stays installed with its data. Nothing moves until you restore.

1. On the phone, open the **old** Workout Logger > Settings > Backup & restore > save a backup. Keep the file somewhere you can find it (Downloads is fine). If there is no Backup & restore in its Settings, stop and tell Claude: an update of the old app can be built from the snapshot first.
2. Android Studio > open this folder > select the phone in the toolbar > Run. "CLEAN Lift" installs as a second app with no data.
3. In CLEAN Lift: Settings (gear) > Backup & restore > choose the file > check the counts > type RESTORE.
4. Compare a few workouts, plans and measurements against the old app.
5. Only when satisfied, uninstall the old Workout Logger. That deletes its data, so the backup file is your fallback.
6. Done 2026-11-20: the project lives in `CLEAN Development\CLEAN Lift`. Open this folder in Android Studio; the old `Desktop\CLEAN Apps` folder is retired and safe to delete once Studio works here.

On the emulator, Run likewise installs CLEAN Lift next to the old app; the same backup/restore applies. Do not uninstall or wipe anything as a shortcut.

## Next work

1. Owner steps above.
2. Remaining review todos: quieter Progress overview (B5), tappable About sheets (B6), Plans FAB (B7), quick-start plans on idle Workout, weight/rep steppers, repeat workout / save history as plan.
3. Launcher icon (L3): owner art.
4. Git: local repo with a first commit (2026-11-20); remote is the public github.com/2AronB/CLEAN_Lift. Commit after each verified slice when the owner asks.

## Batched manual acceptance (emulator, then phone)

1. **Name:** app list shows "CLEAN Lift"; backup save suggests `CLEAN-Lift-<date>.json`; an old-app backup restores fully.
2. **Tabs:** four tabs, Workout first and selected at launch. Switch tabs mid-edit (e.g. a plan draft) and return: draft intact.
3. **Settings gear:** from each tab root; Back (arrow and system Back) returns to the same tab and scroll.
4. **Appearance:** each palette in Light, Dark and System; status bar icons readable; text readable on buttons, chips, cards. Is Iron the right default?
5. **Backup of appearance:** back up with Forest + Dark, switch to Iron + Light, restore: Forest + Dark returns.
6. **Rest:** saving a new set shows 2:00 (or the plan's rest) counting down; +30s adds; Skip hides; correcting an older set does not start rest; survives rotation and tab switches.
7. **Quiet Workout:** removed guidance gone; tapping a set still edits it; TalkBack still announces "Edit set".
8. Large text + small screen: tab labels fit; palette rows wrap.
9. Older pending items (previous-set copy, Metrics daily measurements, backup round trip) remain pending.

Only owner reports complete manual acceptance.

## Historical evidence

- [Before the rebrand](archive/CONTINUE-before-rebrand-2026-11-20.md): 2026-11-19 design pass; 112 JVM / 51 database tests then.
- [Before the design review](archive/CONTINUE-before-design-review-2026-11-19.md), [before proactive engineering](archive/CONTINUE-before-proactive-2026-10-16.md).
- [Earlier baseline](archive/CONTINUE-before-convenience-2026-10-15.md), [convenience slices](archive/CONTINUE-convenience-slices-2026-10-15.md), [original history](archive/CONTINUE-2026-10-15.md).
