# Read this first — current project entry point

This Android project is already built and has a working prototype. Read [docs/CONTINUE.md](docs/CONTINUE.md) for the current status and [AGENTS.md](AGENTS.md) for engineering rules. The material below is historical setup/milestone evidence; do not treat its next-step or unimplemented-feature statements as current instructions. For another app, use the [requirements kit](docs/development/README.md) in a separate folder.

---

# Milestone 0: build and launch

This folder is now the Android project. Open it directly in Android Studio; do not create another project.

## Inspection and verification

The folder initially contained instructions only. Android Studio and the Android SDK were not found in standard locations or registered application entries. Java, Gradle, and adb were unavailable on PATH. JAVA_HOME and SDK environment variables were unset. No usual Gradle cache or emulator configuration was found. Approximately 153 GB of disk space was available. Windows denied hardware queries, so virtualization support remains unverified.

Created a Kotlin + Jetpack Compose + Material 3 app with Workout, Exercises, and History tabs, system light/dark colors, and no workout functionality or storage. The source manifest declares no permissions. Backup is disabled pending future data-safety work. The package com.example.workoutlogger is a development placeholder to finalize before publishing.

Passed: official Gradle wrapper JAR SHA-256 verification, source XML parsing, and source manifest permission check. The Gradle distribution checksum is also pinned.

After Android Studio installation, `:app:assembleDebug :app:lintDebug` passed. Kotlin compilation and dependency resolution passed. The app installed and launched successfully on emulator-5554. A command-driven smoke test verified all three empty screens and Back returning to Workout, then exiting to the launcher. The merged manifest has no INTERNET permission; AndroidX adds only its internal signature-protected DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION. Lint has zero errors and six warnings: older target API, newer Gradle, newer Activity Compose, newer Compose BOM, backup configuration, and missing launcher icon. Dark mode, rotation, and subjective usability still need manual checks.

## Install the missing tools

1. Download the latest stable Windows Android Studio installer from https://developer.android.com/studio . Review and accept the download license yourself.
2. Run it; keep Android Studio and Android Virtual Device selected and use the default installation folder.
3. Open Android Studio. Choose Do not import settings if asked, and Don't send if asked about usage statistics.
4. In the setup wizard, choose Standard, then Next. Review and accept SDK licenses if you agree, then Finish. Wait for downloads to finish. Android Studio includes Java; a separate Java installation is unnecessary.
5. From the welcome screen, click More Actions > SDK Manager. Under SDK Platforms, select Android 16 / API 36. Under SDK Tools, enable Show Package Details and select Android SDK Build-Tools 36.0.0. Also select Android SDK Platform-Tools and Android Emulator. Click Apply, complete the installation, then OK.

Initial tool and library downloads need a connection; the app runs offline.

## Open and build

1. On the welcome screen, click Open. Select `%USERPROFILE%\Desktop\CLEAN Apps`, then OK. Review the project and choose Trust Project if prompted.
2. Wait for Gradle sync and dependency downloads. If offered missing SDK components, install them and review any licenses.
3. If Java compatibility fails, open File > Settings > Build, Execution, Deployment > Build Tools > Gradle. Set Gradle JDK to the bundled jbr / Embedded JDK, then OK. Click File > Sync Project with Gradle Files.
4. Open View > Tool Windows > Terminal. In PowerShell, run:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:lintDebug --console=plain
```

If you installed Studio elsewhere, use that installation's jbr folder. Success is BUILD SUCCESSFUL. The APK is at `app\build\outputs\apk\debug\app-debug.apk`; lint results are at `app\build\reports\lint-results-debug.html`.

## Create and launch an emulator

1. Click View > Tool Windows > Device Manager.
2. Click + > Create Virtual Device (or Create Device).
3. Select Phone > Pixel 8, then Next.
4. Choose Android 16 / API 36, x86_64 for an Intel/AMD Windows computer. A Google APIs image is suitable; a Play Store image is unnecessary. Click its download icon if needed, review the license, and wait for completion.
5. Select the downloaded image, click Next, keep defaults, then Finish.
6. Click the triangle Play button next to the device. Wait for the simulated phone's home screen.
7. In Android Studio's top toolbar, select app as the run configuration and the Pixel 8 as the device. Click the green Run triangle or press Shift+F10. Studio builds, installs, and opens the app.

If the emulator reports a virtualization or hypervisor error, copy its exact text into this chat so we can identify the required Windows or firmware setting.

## Acceptance checklist

- Build and lint end with BUILD SUCCESSFUL.
- Emulator starts and shows its home screen.
- Running app shows Workout Logger and Android setup is working.
- Press Home, then reopen Workout Logger from the phone's app list; the same screen returns.
- Open the emulator's Settings > Display > Dark theme, turn it on, then reopen the app; text remains readable on a dark background.

Report the build result and whether the app launched. Remain at Milestone 0 until these checks pass. No workout features or business-rule tests have been added.

## Pinned toolchain

Android Gradle plugin 9.1.1; Gradle 9.3.1; built-in Kotlin and Compose compiler plugin 2.2.10; Compose BOM 2026.02.01; Activity Compose 1.12.4; compile/target API 36; minimum API 23.

Official references:
- https://developer.android.com/build/releases/agp-9-1-0-release-notes
- https://developer.android.com/develop/ui/compose/compiler
- https://developer.android.com/studio/install
- https://developer.android.com/studio/run/managing-avds

## Milestone 1: app shell

The setup screen has been replaced by three tabs: Workout, Exercises, and History. Each has an empty screen describing functionality planned for later milestones. There are no logging buttons or saved workout data yet. Templates and progress screens are deferred until their features are needed. No libraries or permissions were added.

To test in Android Studio:
1. Open this folder and select app plus your existing Medium Phone emulator in the top toolbar.
2. Click Run (green triangle, or Shift+F10).
3. Confirm Workout opens and shows No active workout.
4. Tap Exercises and History; confirm their titles and selected tab change.
5. Press Android Back from History; confirm it returns to Workout. Press Back again; confirm it leaves the app.
6. Reopen the app, select Exercises, then rotate the emulator with its rotation button. Exercises should stay selected.
7. Open the emulator's Settings > Display > Dark theme, switch it on, and return to the app. Confirm all three tabs remain readable.

Next milestone: local Room data model and database tests, before exercise creation or workout logging.


## Milestones 2 and 3

Room version 1 now provides exercises, workouts, workout exercises, and sets. Exercises supports an alphabetical list, creation, and editing, with blank/duplicate-name validation and discard protection. Workout logging remains deferred.

The six data/validation tests passed. The exercise UI requires the manual checks in docs/MILESTONES-2-3-TESTS.md; command-injected screen input was inconclusive, and the attempted UI test was removed at your request. No sample exercise was successfully saved during that attempt. Room/KSP, AndroidX lifecycle, JUnit, and the AndroidX database test runner are the required new dependencies. There is no production networking dependency or INTERNET permission.

Open docs/MILESTONES-2-3-TESTS.md for the manual acceptance checklist and docs/LOCAL-DATA.md for the schema and durability decisions. The gradle.properties setting keeps app installations after connected database tests so normal test cleanup does not remove emulator data.

## Milestone 4: basic workout logging

Implemented Start workout, searchable exercise selection, per-set lb/kg weight and reps, save/edit/remove sets, remove exercises, finish, and confirmed cancellation. Each saved set is written to Room immediately. Unfinished workouts resume after reopening; the Workout tab shows the latest completed workout when no session is active. Full history browsing remains Milestone 5. Tags, templates, RPE/notes entry, timers, and charts remain deferred.

The app installs 31 common strength exercises once, preserving existing names and IDs and avoiding normalized-name duplicates. Schema migration 1 to 2 adds only app_metadata for that one-time marker; it does not rebuild or drop workout/exercise tables. Both exported schemas are retained.

Use docs/MILESTONE-4-TESTS.md for the complete manual acceptance checklist. Screen/keyboard usability testing is left to the user. Automated checks cover validation, database durability, lifecycle rules, and migration safety.

Verification for Milestone 4: debug build and Kotlin compilation passed; 4 JVM validation tests and 10 Android database/migration tests passed on the desktop emulator; lint passed with zero errors. The merged manifest still has no INTERNET permission. No UI automation was performed for this milestone. Screen, keyboard, and subjective usability checks remain in docs/MILESTONE-4-TESTS.md.

## Milestone 5: history and faster set entry

Save set now stays in the editor, retains weight/reps/unit, advances the set number, and shows saved sets below for editing. Next exercise is a separate top-right action that opens the picker for empty-start workouts. The active screen shows newest exercises/sets first, with Add exercise above and Add set beside each exercise name.

History lists completed workouts newest first, opens their details, and allows correcting saved weight/reps/unit. Corrections preserve all other fields and do not create new sets. The existing version-2 schema and dependencies remain unchanged. Supersets and tags are recorded in Ideas.md for later; templates are Milestone 6. Future template-based Next exercise will follow the template order.

Verification: debug build, Kotlin compilation, 4 JVM tests, and 13 Android database/migration tests passed on Medium_Phone_API_37.0. Lint passed with zero errors. No UI automation was performed. Install the update using Run > Run app (Shift+F10), then use docs/MILESTONE-5-TESTS.md for manual acceptance.

Milestone 5 was accepted: the user reported all checks 1–9 passed.

## Milestones 6 and 7: templates and guided workouts

Templates provides a simple named-workout builder with ordered exercises and fixed set counts or Until done. Start follows the saved order; Continue planned workout resumes progress. Gray plan actions can be held to override just that action, with an override count shown in the active workout and history. Empty-start workouts keep their existing picker flow. Finish workout is now at the exercise picker's top right.

Schema migration 2 to 3 adds two template tables and plan snapshot/progress fields while preserving existing data; exported schemas 1–3 remain available. No new dependencies or permissions were added. Template edits/deletion leave active and historical workouts intact. UI/keyboard/long-press acceptance remains manual: use docs/MILESTONES-6-7-TESTS.md. See docs/ROADMAP.md for accepted work, the current scope, and proposed later milestones; supersets and tags remain deferred.

Verification: debug build/Kotlin compilation, 7 JVM tests and 20 Android database/migration tests passed on the desktop Medium_Phone_API_37.0 emulator. Lint has zero errors and 12 existing warnings. The merged manifest has no INTERNET permission. No automated UI interactions were performed.

## Current iteration: history, progress, body tracking and template convenience

History now supports literal name search, named-template summaries, Exercise progress and Body measurements. Progress has time/unit views, heaviest-load plots and dated 5/10-rep estimated 1RM records/plots. A break scenario is an optional user assumption, never a predicted personal loss or a change to recorded history. Body tracking supports dated bodyweight and circumference entries with edit/delete and display-unit conversion. Templates can be duplicated into an editable draft. Set entry can show and explicitly copy dated previous-workout values while preserving current-workout defaults and protecting changed drafts.

Schema 4 adds only body_measurements; all previous records/plan/template data remain. No dependencies or permissions were added. Existing launcher artwork is preserved. Build/lint and 15 JVM + 26 Android database/migration tests passed. Lint has zero errors; 17 warnings include pre-existing toolchain, backup, plurals and launcher-resource checks. Final navigation/layout polish also passed build/JVM/lint; it does not change database behavior. No UI automation or physical-device verification was performed.

Install via Run > Run app (Shift+F10), then use docs/ITERATION-1-TESTS.md. If the emulator restores a crashing quick-boot snapshot, Device Manager > device three-dot menu > Cold Boot Now preserves user data; do not choose Wipe Data. The emulator was recovered this way during automated database verification.

Future agents: read AGENTS.md then docs/CONTINUE.md and only the relevant docs/features note. scripts/verify.ps1 standardizes available checks. Equipment-aware generation has an implementation plan in docs/features/EQUIPMENT-PLANNER-DESIGN.md; it is not working UI yet. No SDK/emulator setup change is otherwise required.

