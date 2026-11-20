# Milestone 4: log a complete workout

Milestone 4 was accepted by the user. The updated set-entry behavior and history checks are in [MILESTONE-5-TESTS.md](MILESTONE-5-TESTS.md); use that checklist for the current version. The steps below describe the earlier Milestone 4 interface.

## Install the update

1. Open this folder in Android Studio.
2. Click File > Sync Project with Gradle Files if Studio requests it.
3. Select app and your Medium Phone emulator in the top toolbar.
4. Click Run (green triangle), or press Shift+F10. If the app is already running, use Run > Run app to install this update.

The app adds 31 common exercises once. Existing exercises are preserved; matching names are reused. Renaming a starter exercise will not recreate its original name on the next launch. Tags remain deferred.

## Main acceptance test

1. Open Exercises. Confirm your previous custom names/edits are still present, along with common lifts such as Squat, Deadlift, and Bench Press.
2. Open Workout and tap Start workout.
3. Tap Add exercise. Search Bench, then tap Bench Press (or your renamed bench exercise).
4. Tap Add set. Choose Pounds (lb), enter weight 185 and reps 8, then tap Save set.
5. Repeat for 185 x 7 and 175 x 8. Confirm all three saved sets appear in order.
6. Tap Add exercise, search Squat, and select it. Save two sets: 135 lb x 10 and 155 lb x 8.
7. Tap Finish workout. Confirm Latest completed workout shows both exercises and all five sets with the correct units and reps.
8. Force stop the app: emulator Settings > Apps > See all apps > Workout Logger > Force stop > OK. Reopen Workout Logger from the app list. Confirm the complete five-set workout still appears exactly as saved.

## Additional checks

- Recovery before finishing: start another workout, add a lift and save a set. Force stop/reopen. Confirm Active workout resumes with the saved set and no second active workout is created.
- Edit: tap a saved set during an active workout, change weight and reps, and Save set. Confirm the same set changes without creating an extra one.
- Removal: save three sets, remove the middle one and confirm. Add another set; confirm the others remain correct and ordered. Remove an exercise and confirm only its own sets disappear.
- Finish validation: Finish workout should be disabled for an empty session or an exercise with no saved sets. Save a set or remove the unused exercise to enable it.
- Cancel: tap Cancel workout. Choose Keep first; data should stay. Repeat and choose Discard. Confirm only the active workout disappears and the last completed workout is still shown.
- Input validation: blank weight/reps, negative weight, zero reps, decimal reps, more than three weight decimals, and more than 999 reps must not save. Zero weight with positive reps is allowed for bodyweight. Try 72.125 kg x 8 and confirm those exact values after finishing/reopening.
- Unit selection: each set displays the unit you chose. Changing lb/kg in the editor changes the unit for the number entered; it does not convert the number automatically.
- Drafts: type an unsaved set, rotate, and confirm the draft remains. Press Back/Cancel and verify Keep/Discard choices. Values become durable only after Save set; force-stopping an unsaved editor may lose its draft.
- Offline: enable airplane mode and record/finish a workout. Confirm it works without a connection.
- Dark mode and keyboard: try portrait/landscape and dark mode. Confirm fields, Save set, and Finish remain reachable. The keyboard Next action moves from weight to reps; Done saves valid values.
- Custom exercises: if an exercise is missing, leave the picker, create it in Exercises, return to Workout, and add it to the still-active session.

The Workout tab shows the most recent completed workout when there is no active workout. Older completed workouts are retained in the database; browsing them is Milestone 5. No templates, tags, rest timer, RPE entry, or analytics have been added.

Do not uninstall or clear app storage to test persistence: those actions delete local data.
