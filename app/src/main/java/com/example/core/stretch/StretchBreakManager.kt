package com.example.core.stretch

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * StretchBreakManager: Manages exercise selection, shuffling, skip tracking,
 * daily streak counting, and rule enforcement (zone variety, sitting-only filtering, and short mode).
 */
object StretchBreakManager {

    private var breakCounter: Int = 0
    private var lastShuffledIds: Set<String> = emptySet()
    private val consecutiveSkipCounts = mutableMapOf<String, Int>()
    private val suppressedUntilBreak = mutableMapOf<String, Int>()

    // Daily streak tracker
    private var todayDateKey: String = ""
    private var todayBreakCount: Int = 0

    private fun checkDateReset() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (today != todayDateKey) {
            todayDateKey = today
            todayBreakCount = 0
        }
    }

    /**
     * Returns the number of breaks completed today (resets daily).
     */
    fun getTodayBreakCount(): Int {
        checkDateReset()
        return todayBreakCount
    }

    /**
     * Increments the today's break count and returns the updated count.
     */
    fun recordBreakCompleted(): Int {
        checkDateReset()
        todayBreakCount++
        return todayBreakCount
    }

    /**
     * Called when an exercise is skipped to update consecutive skip counts.
     * If skipped 3 times, suppress for 5 breaks.
     */
    fun recordExerciseSkipped(exerciseId: String) {
        val currentSkips = (consecutiveSkipCounts[exerciseId] ?: 0) + 1
        consecutiveSkipCounts[exerciseId] = currentSkips
        if (currentSkips >= 3) {
            // Suppress this exercise for the next 5 breaks
            suppressedUntilBreak[exerciseId] = breakCounter + 5
            consecutiveSkipCounts[exerciseId] = 0
        }
    }

    /**
     * Called when an exercise is completed to reset its consecutive skip counter.
     */
    fun recordExerciseCompleted(exerciseId: String) {
        consecutiveSkipCounts[exerciseId] = 0
    }

    /**
     * Generates the ordered exercise list for the current stretch break session.
     */
    fun getExercisesForBreak(
        shortMode: Boolean,
        includeStanding: Boolean
    ): List<StretchExercise> {
        breakCounter++

        // Base fixed exercises (4 exercises)
        val fixedList: MutableList<StretchExercise> = if (includeStanding) {
            ExerciseCatalog.fixedExercises.toMutableList()
        } else {
            // If standing exercises disabled: replace standing ones with seated posture hold & ankle pumps
            mutableListOf(
                ExerciseCatalog.fixedExercises[0], // Chin tuck (NECK)
                ExerciseCatalog.fixedExercises[1], // Wrist stretch (SHOULDER_HAND)
                ExerciseCatalog.seatedPostureHold, // Seated posture hold (BACK)
                ExerciseCatalog.anklePumps         // Ankle pumps (LEGS)
            )
        }

        // Available pool for shuffling (filters out suppressed and standing if sitting-only)
        val candidatePool = ExerciseCatalog.shuffledPool.filter { exercise ->
            if (!includeStanding && exercise.isStanding) {
                false
            } else {
                val suppressUntil = suppressedUntilBreak[exercise.id] ?: 0
                breakCounter > suppressUntil
            }
        }

        // Rotate shuffled exercises so user sees different ones each break (exclude immediate last)
        val nonRepeatedPool = candidatePool.filter { !lastShuffledIds.contains(it.id) }
        val finalCandidatePool = if (nonRepeatedPool.isNotEmpty()) nonRepeatedPool else candidatePool

        if (shortMode) {
            // Short mode: 3 exercises = Chin tuck, Wrist stretch, and 1 shuffled exercise
            val pickedShuffled = finalCandidatePool.shuffled().firstOrNull() ?: ExerciseCatalog.shuffledPool.first()
            lastShuffledIds = setOf(pickedShuffled.id)
            return listOf(
                ExerciseCatalog.fixedExercises[0], // Chin tuck
                ExerciseCatalog.fixedExercises[1], // Wrist stretch
                pickedShuffled
            )
        }

        // Full mode: pick 2 shuffled exercises from DIFFERENT zones
        val shuffledCandidates = finalCandidatePool.shuffled()
        val pickedList = mutableListOf<StretchExercise>()

        for (ex in shuffledCandidates) {
            if (pickedList.isEmpty()) {
                pickedList.add(ex)
            } else if (pickedList.size == 1 && pickedList[0].zone != ex.zone) {
                pickedList.add(ex)
                break
            }
        }

        // Fallback if distinct zone candidate was not found
        if (pickedList.size < 2 && shuffledCandidates.size >= 2) {
            pickedList.clear()
            pickedList.addAll(shuffledCandidates.take(2))
        }

        lastShuffledIds = pickedList.map { it.id }.toSet()

        // Combine fixed (4) + shuffled (2) = 6 exercises
        val fullBreakList = mutableListOf<StretchExercise>()
        fullBreakList.addAll(fixedList)
        fullBreakList.addAll(pickedList)
        return fullBreakList
    }
}
