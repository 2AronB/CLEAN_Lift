# Current continuation checkpoint

Read AGENTS.md, PRODUCT/ARCHITECTURE/DECISIONS/DEVELOPMENT and the relevant feature contract. Archived checkpoints are historical evidence.

## Standing authorization

Unchanged from 2026-10-16 (D-011): proactive offline improvements toward the workout app Android should have shipped with, within the dependency/permission/service/network approval boundaries. 2026-11-19 adds D-012: follow `CLEAN Development\DESIGN-PREFERENCES.md`; four-tab navigation and palettes are accepted.

## Current change — 2026-11-19: design review pass (D-012)

Review with todos, issues and the rebrand plan: [REVIEW-2026-11-19](REVIEW-2026-11-19.md). Implemented:

- **Navigation:** Workout / Plans / Progress / Tools (internal Workout / Templates / Metrics / Widgets). Settings is a gear on each tab root; Back returns to the origin tab with its state. Text "Back" buttons replaced by a native arrow everywhere. Progress' own settings page is now "Customize".
- **Appearance:** Settings > Appearance with Theme (System/Light/Dark) and Palette (Iron default, Chalk, Forest, Wallpaper on Android 12+). Status-bar icons follow the chosen mode. Saved as `palette`/`theme` in `local-profile`; backups include and validate them; older backups restore to defaults. Semibold titles.
- **Quieter screens:** removed repeated guidance ("Saved sets are stored…", "Tap Save set…", "Tap to edit" on rows, override explanation outside the active workout), the disabled delete-all placeholder, and link-styled Tools rows. Start workout is a full-width large button. "Template" wording is now "plan".
- **Rest countdown in Workout:** starts when a new set is saved (not when correcting a set), uses the plan's rest or 2:00, +30s/Skip, lingers 10 s as "Rest done". Visible screen only; not stored.

No schema change (still 8), no dependency/permission/service change. Pre-change source snapshot: `tmp/snapshot-2026-11-19/`.

Verification actually run 2026-11-19: `scripts/verify.ps1` passed debug build, 112 JVM tests (new AppearanceTest including a 4.5:1 contrast check on every built-in palette, new RestTimerTest) and lint with 0 errors / 33 warnings. The two extra warnings over the October count of 31 are most likely newer library-version notices (for example Room 2.8.5, Compose BOM 2026.11.01); none are in changed code. `scripts/verify.ps1 -DatabaseTests` then ran all 51 isolated tests on emulator-5554 (Medium Phone API 37): 0 failures/errors/skipped, Gradle test exit code 0. BackupDataTest now also round-trips `palette`/`theme` and rejects an unknown palette and a non-string theme. The PowerShell wrapper reported exit 1 only because the invocation piped stderr (`2>&1`), which makes PowerShell 5.1 treat Gradle's JDK warning as an error under `$ErrorActionPreference = 'Stop'`; run the script without `2>&1`. No UI automation; no phone interaction.

## Next work

1. Owner: pick the CLEAN name, then run the rebrand plan in the review (backup on phone first).
2. Remaining review todos: quieter Progress overview (B5), tappable About sheets for long Progress text (B6), Plans FAB (B7), quick-start plans on idle Workout, weight/rep steppers, repeat workout / save history as plan.
3. Recommend `git init` and a first local commit before further changes.

## Batched manual acceptance (emulator, then phone)

Android Studio > open this folder > View > Tool Windows > Device Manager > start the existing Medium Phone > select it in the toolbar > Run. Do not wipe, uninstall or clear storage.

1. **Tabs:** four tabs, Workout first and selected at launch. Switch tabs mid-edit (e.g. a plan draft) and return: draft intact.
2. **Settings gear:** from each tab root, tap the gear; Back (arrow and system Back) returns to the same tab, same scroll.
3. **Appearance:** try each palette in Light and Dark and System; status bar icons stay readable; text readable on buttons, chips and cards. Is Iron the right default? Which palette do you like?
4. **Backup:** make a backup after choosing Forest + Dark, switch to Iron + Light, restore: Forest + Dark returns.
5. **Rest:** in a workout, save a set: rest card shows 2:00 (or the plan's rest) counting down; +30s adds; Skip hides; correcting an older set does not start rest; rotate mid-rest: continues. Leave to another tab and back: still correct.
6. **Quiet Workout:** active workout and set editor no longer show the removed guidance; tapping a set still edits it; TalkBack still announces "Edit set".
7. Large text + small screen: tab labels fit; Settings palette rows wrap.
8. Older pending items from the archived checkpoint (previous-set copy, Metrics daily measurements, backup round trip) remain pending.

Only owner reports complete manual acceptance.

## Historical evidence

- [Before this review](archive/CONTINUE-before-design-review-2026-11-19.md): 2026-10-16 previous-set reference; 105 JVM / 51 database tests then.
- [State before proactive engineering](archive/CONTINUE-before-proactive-2026-10-16.md).
- [Earlier baseline](archive/CONTINUE-before-convenience-2026-10-15.md), [convenience slices](archive/CONTINUE-convenience-slices-2026-10-15.md), [original history](archive/CONTINUE-2026-10-15.md).
