package com.example

import com.example.core.timer.TimerEngine
import com.example.core.timer.TimerState
import com.example.core.timer.TimerType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TimerEngineTest {

    @Before
    fun setUp() {
        TimerEngine.reset()
    }

    @Test
    fun testInitialStateIsIdle() {
        val snap = TimerEngine.snapshot.value
        assertEquals(TimerState.IDLE, snap.state)
    }

    @Test
    fun testStartStudySessionTransitionsToStudying() {
        TimerEngine.startStudySession(
            subjectId = 1,
            topicId = 2,
            subjectName = "English Literature",
            topicName = "Romantic Poetry",
            type = TimerType.STUDY_SESSION,
            durationMinutes = 50
        )

        val snap = TimerEngine.snapshot.value
        assertEquals(TimerState.STUDYING, snap.state)
        assertEquals("English Literature", snap.subjectName)
        assertEquals("Romantic Poetry", snap.topicName)
        assertEquals(50 * 60 * 1000L, snap.plannedDurationMillis)
        assertTrue(snap.sessionUid.isNotEmpty())
    }

    @Test
    fun testPauseAndResume() {
        TimerEngine.startStudySession(
            subjectId = 1,
            topicId = 2,
            subjectName = "Chemistry",
            topicName = "Organic Chemistry",
            durationMinutes = 25
        )

        TimerEngine.pause()
        var snap = TimerEngine.snapshot.value
        assertEquals(TimerState.PAUSED, snap.state)
        assertEquals(1, snap.interruptionCount)

        TimerEngine.resume()
        snap = TimerEngine.snapshot.value
        assertEquals(TimerState.STUDYING, snap.state)
    }

    @Test
    fun testStopTransitionsToStopped() {
        TimerEngine.startStudySession(
            subjectId = 1,
            topicId = 2,
            subjectName = "Mathematics",
            topicName = "Calculus",
            durationMinutes = 30
        )

        TimerEngine.stop()
        val snap = TimerEngine.snapshot.value
        assertEquals(TimerState.STOPPED, snap.state)
    }

    @Test
    fun testEyeCareTriggerAndAutoResume() {
        TimerEngine.startStudySession(
            subjectId = 1,
            topicId = 2,
            subjectName = "Computer Science",
            topicName = "Algorithms",
            durationMinutes = 50
        )

        // Manually trigger eye alert
        TimerEngine.triggerEyeRestAlert()
        var snap = TimerEngine.snapshot.value
        assertEquals(TimerState.EYE_REST, snap.state)
        assertTrue(snap.state.isBreak)

        // Complete eye rest
        TimerEngine.completeEyeRest(autoResumed = true)
        snap = TimerEngine.snapshot.value
        assertEquals(TimerState.STUDYING, snap.state)
    }

    @Test
    fun testHydrationReminderAndDrankWater() {
        TimerEngine.startStudySession(
            subjectId = 1,
            topicId = 2,
            subjectName = "History",
            topicName = "Modern Era",
            durationMinutes = 60
        )

        // Trigger hydration reminder
        TimerEngine.triggerWaterReminder()
        var snap = TimerEngine.snapshot.value
        assertEquals(TimerState.WATER_BREAK, snap.state)

        // Confirm drank water
        TimerEngine.confirmDrankWater()
        snap = TimerEngine.snapshot.value
        assertEquals(TimerState.STUDYING, snap.state)
    }

    @Test
    fun testStopwatchLaps() {
        TimerEngine.startStudySession(
            subjectId = 1,
            topicId = 2,
            subjectName = "Physics",
            topicName = "Kinematics",
            type = TimerType.STOPWATCH
        )

        assertEquals(TimerType.STOPWATCH, TimerEngine.snapshot.value.timerType)
        TimerEngine.recordLap()
        val snap = TimerEngine.snapshot.value
        assertEquals(1, snap.laps.size)
        assertEquals(1, snap.laps.first().lapNumber)
    }

    @Test
    fun testDebugTestModeToggling() {
        TimerEngine.debugModeActive = true
        assertTrue(TimerEngine.debugModeActive)
        assertEquals(20L, TimerEngine.eyeStudyIntervalSeconds)
        assertEquals(5L, TimerEngine.eyeRestDurationSeconds)
        assertEquals(30L, TimerEngine.waterIntervalSeconds)

        TimerEngine.debugModeActive = false
        assertFalse(TimerEngine.debugModeActive)
        assertEquals(20 * 60L, TimerEngine.eyeStudyIntervalSeconds)
        assertEquals(20L, TimerEngine.eyeRestDurationSeconds)
        assertEquals(45 * 60L, TimerEngine.waterIntervalSeconds)
    }
}
