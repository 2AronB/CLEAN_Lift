# Milestone 5: faster set entry and workout history

Accepted: the user reported that all manual checks 1–9 passed. Continue with MILESTONES-6-7-TESTS.md for the builder and guided-workout update.

## Install this update

1. Open `%USERPROFILE%\Desktop\CLEAN Development\CLEAN Lift` in Android Studio.
2. Select `app` and your existing Medium Phone emulator in the top toolbar.
3. Choose **Run > Run app**, or press **Shift+F10**, even if the previous app version is already open. Wait for installation to finish.

No SDK, emulator, dependency, or database setup changes are needed. Existing exercises and workouts should remain present.

## Acceptance checklist

1. **Repeated set entry:** start an empty workout, tap Add exercise, search Bench and select Bench Press (or your renamed bench exercise). The set editor opens. Enter **185 lb × 8**, then tap **Save set**. Stay in the editor: the saved row appears below, weight/reps remain **185 / 8**, and the heading advances to **Set 2**. Save again, then change reps to 7 and save. Expect exactly three sets, with the newest first and heading **Set 4**.
2. **Edit without duplication:** tap saved **Set 1**, change weight to **190**, and tap **Save changes**. Expect three saved rows, Set 1 now **190 lb × 8**, and the heading back in append mode at Set 4. Save another set to confirm editing did not replace a different row.
3. **Next exercise:** tap top-right **Next exercise**. Expect the picker, no exercise selected automatically, and no extra set saved from the retained values. Search Squat and select it. Expect a blank Set 1 for Squat. Save **135 lb × 10**. Tap Back to return to the active workout.
4. **Active layout and defaults:** Add exercise should appear above the exercises, Squat above Bench Press, Add set beside each exercise name, and newer sets above older sets. Tap Add set beside Bench Press: expect its latest saved weight/reps/unit, not Squat's values.
5. **Unsaved changes:** change a value and tap Next exercise. Choose Keep: remain in the editor with the draft intact. Try again and choose Discard: open the picker without adding a set. Also change a value and tap a saved row below; Keep should preserve the draft, and Discard should open the tapped row's existing values. Finish any exercise with no sets by adding a set or removing that exercise.
6. **History:** return to the active screen and finish the workout. Open History. Expect the new workout first and earlier completed workouts below. Open this workout and an older one. Check exercise names, chronological set numbering, counts, exact weights, reps, and units. Back returns from details to History; Back again returns to Workout.
7. **History correction:** open an older workout, tap a set, change weight/reps and Save changes. Expect the same set updated with no added row. Other sets and the other workout should remain unchanged. Back out of another unsaved correction and choose Keep editing/Discard to check both choices. Starting an unfinished workout should not add it to History.
8. **Persistence:** in the emulator open Settings > Apps > See all apps > Workout Logger > Force stop > OK. Reopen Workout Logger from the app list. Check History still contains the correction and the new completed workout. If you left an active workout, confirm it resumes with its saved sets.
9. **Keyboard and display:** rotate while a changed set draft is open; confirm draft and selected set remain. Try dark mode and the open keyboard: Save set/Save changes and saved rows should remain reachable by scrolling. Keyboard Next moves weight to reps; Done saves once. Check blank/negative weight and zero reps cannot save. Optionally enable airplane mode and repeat one save/correction.

Changing lb/kg relabels the entered number; it does not convert it. Do not uninstall or clear storage to test persistence.

History initially loads up to 50 summaries, with Show more for older entries. No need to manufacture 50 workouts for this manual check: query ordering/limits/counts are covered by database tests. UI and subjective usability checks are left to you; automated checks cover compilation, lint, validation, data preservation, and migration.

Next milestone: **6 — reusable workout templates**. Template-based Next exercise will follow the template's explicit exercise order. Workouts started empty continue to open the picker.
