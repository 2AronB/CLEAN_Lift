# Milestones 2 and 3: manual checks

Open this project in Android Studio. Click File > Sync Project with Gradle Files after the dependency changes. Select app and your Medium Phone emulator, then click the green Run triangle (Shift+F10).

1. Open Exercises. Tap New exercise, enter Bench Press, and tap Save. Confirm one row appears.
2. Add Squat and Deadlift. Confirm alphabetical order: Bench Press, Deadlift, Squat.
3. Try an empty name and a name containing only spaces. Save should be disabled. Try more than 100 letters; saving should be blocked.
4. Try creating bench press with different capitalization and surrounding spaces. Save should show a duplicate-name message; no extra row should appear. Correct the name and save successfully.
5. Tap Bench Press, rename it Barbell Bench Press, and save. Confirm there is only one renamed row.
6. Edit Squat to match Deadlift. Confirm the duplicate is rejected and Squat remains unchanged. Tap Cancel, then Discard to leave the unsaved edit.
7. Open an exercise, change its name, and tap Cancel. Choose Keep editing; confirm your draft remains. Cancel again and choose Discard; confirm the stored name is unchanged.
8. Start a new exercise and type Incline Bench Press without saving. Rotate the emulator. Confirm the draft and editor remain. Save once and confirm exactly one row.
9. Hide the keyboard with Android Back, then press Back again from an edited draft. Confirm the discard prompt appears. From the list, Back returns to Workout.
10. Check persistence: in the emulator open Settings > Apps > See all apps > Workout Logger > Force stop > OK. Open Workout Logger again from the phone's app list. Confirm all saved exercises and edits remain. Do not select Clear storage or uninstall the app.
11. Enable airplane mode from the emulator's quick settings (swipe down from the top twice). Reopen the app and create/edit an exercise. Confirm it works offline.
12. Turn on Settings > Display > Dark theme. Check the list, editor, duplicate error, and discard prompt. Try the emulator's larger font setting under Settings > Display > Display size and text; names and Save/Cancel should remain usable.

Workout and History remain empty until their later milestones. There is no exercise deletion or category field in this milestone.

To rerun automated checks from Android Studio's PowerShell Terminal:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain
```

Keep the emulator running for the Android database tests.
