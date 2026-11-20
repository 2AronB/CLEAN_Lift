# Settings and CLEAN promise

Ownership: `ui/SettingsScreen.kt`, `ui/SettingsViewModel.kt`, root navigation in `ui/WorkoutLoggerApp.kt`, Workout top-bar entry, strings in `res/values/strings.xml`.

Settings is now the fifth primary tab, also reachable from Workout's top-bar shortcut. Back returns to the existing Workout. The optional local profile stores a trimmed name (up to 80 characters), age (blank or 1–120 whole years), and gender (blank, M or F) in private `local-profile` SharedPreferences. Gender defaults to Not set for existing installations; the profile editor offers Not set/M/F chips and can clear the saved choice. Missing or unrecognized stored choices load as Not set. Drafts survive rotation and changing gender participates in the discard confirmation. Saving runs off the UI thread, checks commit success, and retains the draft on failure. Metrics uses profile gender for sex-specific equations and reference ranges, and profile age as an editable starting value for age at measurement. Each result saves its own inputs; profile changes never rewrite old results. See METRICS.md. The profile itself introduces no Room changes, permissions, dependencies or services.

Ads, trackers and purchases have no functional settings or SDKs. There is no explanatory header above the switches. Each label includes (Off) or (Turning on). Every attempt resets after five seconds; attempts one and two are silent, attempt three responds with exactly "No. Stop that.", and every later attempt cycles through four relevant jokes per feature. Each independent sequence is remembered in private preferences, including across app restarts; no enabled feature state is stored. Concurrent attempts are blocked. The saved monotonic start time preserves the remaining delay through rotation. Android switch animations and accessible labels remain standard Material behavior.

Settings > Backup & restore now saves a readable versioned local JSON file and validates/previews replacement restore. The user explicitly authorized both on 2026-10-15. See [PORTABILITY.md](PORTABILITY.md) for data coverage, confirmation, interrupted-restore recovery, compatibility limits and manual checks. Delete-all is not implemented; its disabled placeholder row was removed from Settings on 2026-11-19 (quiet-screen review). Implementing it is not authorized by that change. Any future deletion must cover Room and profile with explicit confirmation. Android automatic backup stays disabled.

Automated verification: profile bounds/optional input and switch attempt sequence unit tests plus project build, JVM tests and lint. Manual UI acceptance remains required.

Manual acceptance:
1. Open Android Studio, open this project, select the running emulator in the top device selector and click the green Run triangle.
2. On Workout tap Settings > Edit profile. Confirm gender initially shows Not set. Select M, Save, close/reopen the app and confirm it persisted with name/age unchanged. Repeat with F, then Not set. Change gender and press Back to confirm discard/keep-editing appears; rotate while editing to check the draft is preserved. Clear name/age and Save to confirm all fields are optional.
3. Enter age 0, 121 or a decimal: Save must be disabled. Edit a field and press Back: discard/keep-editing must appear.
4. Scroll to the bottom. On a feature that has not been attempted in this version, try its switch twice: each time the label changes to (Turning on), then back to (Off) after five seconds without a dialog. The third attempt resets after five seconds and shows "No. Stop that.". Every later attempt shows a relevant joke. Dismiss with Keep it CLEAN. Repeat for the other switches; their counts are independent.
5. Reopen the app after the third attempt and confirm the next attempt shows a joke without another silent introduction. Rotate during the five-second wait and confirm the remaining wait is preserved. Rotate during the dialog; dismiss and confirm all switches end off. Press Back to return to Workout, including with a workout in progress.
6. Verify Backup & restore opens the working portability screen, and no delete-all row appears. Check small-screen scrolling, large text, dark mode and TalkBack switch labels.

Next action: manual acceptance of profile/preferences and the PORTABILITY checklist. Do not enable delete-all or select another backlog feature by default.

## Appearance — 2026-11-19 (D-012)

Settings opens from the gear on each tab root (not a tab). Back returns to the tab it was opened from. The Appearance section offers Theme (System / Light / Dark) and Palette (Iron, Chalk, Forest, and Wallpaper on Android 12+), each row previewing three colors. Changes apply only after the preference commit succeeds; a failed write shows the standard save error. Keys `palette` and `theme` in `local-profile` are included in backups and validated on restore; status-bar icons follow the chosen mode.

