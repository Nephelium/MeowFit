# Dark Calendar, Navigation, and Overflow Progress Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Execute this plan task-by-task in the current clean worktree.

**Goal:** Preserve calendar metric colors in dark mode, modernize bottom navigation, and visualize unlimited goal overflow with cycling colors.

**Architecture:** Keep calendar color selection in the existing Compose heatmap function while replacing the shared green dark scale with per-metric gradients. Add a pure Kotlin progress-cycle policy in `domain` and a reusable Canvas progress component in `ui/components`; replace only the homepage summary bar. Replace hand-drawn navigation icons with Material icons without changing routes.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, JUnit 4, Gradle Android plugin.

---

### Task 1: Establish the UI rule and regression specification

**Files:**
- Modify: `AGENT.md`
- Create: `docs/plans/2026-07-26-dark-calendar-navigation-overflow-design.md`
- Create: `docs/plans/2026-07-26-dark-calendar-navigation-overflow.md`

**Steps:**
1. Record bottom navigation as the explicit non-pixel exception.
2. Document metric colors, navigation appearance, progress-cycle behavior, and manual checks.
3. Verify the files are UTF-8 and visible in `git diff`.

### Task 2: Add tested progress-cycle logic

**Files:**
- Create: `app/src/main/java/com/example/calorietracker/domain/CyclicProgressPolicy.kt`
- Create: `app/src/test/java/com/example/calorietracker/domain/CyclicProgressPolicyTest.kt`

**Steps:**
1. Write tests for zero, partial, exact-cycle, overflow, multi-cycle, and invalid values.
2. Run `.\gradlew.bat testDebugUnitTest --tests "*CyclicProgressPolicyTest"` and confirm the missing implementation fails.
3. Implement a finite, non-negative ratio normalizer that returns completed cycles and current-cycle fraction.
4. Re-run the focused test and expect it to pass.

### Task 3: Render cycling overflow on the homepage

**Files:**
- Create: `app/src/main/java/com/example/calorietracker/ui/components/CyclicProgressBar.kt`
- Modify: `app/src/main/java/com/example/calorietracker/ui/screens/TodayScreen.kt`

**Steps:**
1. Draw the track, last completed cycle, and current partial cycle in a clipped rounded Canvas.
2. Add accessibility text with the uncapped percentage.
3. Replace the capped `LinearProgressIndicator` in `SummaryCard`.
4. Compile the debug variant.

### Task 4: Restore per-metric dark calendar colors

**Files:**
- Modify: `app/src/main/java/com/example/calorietracker/ui/screens/OverviewScreen.kt`

**Steps:**
1. Replace the common green dark scale with metric-specific gradients matching light-mode semantics.
2. Keep green/red only for heat deficit/surplus.
3. Preserve existing background-based content color selection.
4. Compile and inspect the focused diff.

### Task 5: Modernize bottom navigation

**Files:**
- Modify: `app/src/main/java/com/example/calorietracker/MainActivity.kt`
- Delete: `app/src/main/java/com/example/calorietracker/ui/components/PixelNavigation.kt`

**Steps:**
1. Map the five existing routes to Material icons.
2. Render them in a rounded elevated container with pill selection state.
3. Keep route restoration, system navigation padding, and accessibility semantics unchanged.
4. Delete the now-unused pixel icon source after receiving explicit permission.

### Task 6: Verify and package

**Files:**
- Verify: `app/build/outputs/apk/debug/MeowFit-v2.0.2-debug.apk`

**Steps:**
1. Run `.\gradlew.bat testDebugUnitTest`.
2. Run `.\gradlew.bat assembleDebug`.
3. Inspect APK existence, size, hash, package metadata, and repository status.
4. List Android Studio manual checks for dark/light themes and overflow ratios.
5. Stop for explicit authorization before commit/push and GitHub release upload.
