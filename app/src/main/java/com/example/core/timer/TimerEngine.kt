package com.example.core.timer

import android.os.SystemClock
import com.example.core.logging.AppLogger
import com.example.core.logging.LogFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

object TimerEngine {
    private val scope = CoroutineScope(Dispatchers.Default)
    @Volatile
    private var tickerJob: Job? = null

    // Configuration defaults
    var eyeCareEnabled: Boolean = true
    var eyeStudyIntervalSeconds: Long = 20 * 60L // 20 minutes study interval
    var eyeRestDurationSeconds: Long = 20L // 20 seconds break countdown
    var eyeAutoResumeEnabled: Boolean = true

    var waterReminderEnabled: Boolean = true
    var waterIntervalSeconds: Long = 45 * 60L // 45 minutes study interval
    var waterBreakDurationSeconds: Long = 5 * 60L // 5 minutes break countdown (300s)
    var waterAutoResumeEnabled: Boolean = true
    var waterRemindLaterSeconds: Long = 5 * 60L

    private val breakQueue = ArrayDeque<BreakType>()

    var debugModeActive: Boolean = false
        set(value) {
            field = value
            if (value) {
                eyeStudyIntervalSeconds = 20L
                eyeRestDurationSeconds = 5L
                waterIntervalSeconds = 30L
                waterBreakDurationSeconds = 5L
                waterRemindLaterSeconds = 10L
            } else {
                eyeStudyIntervalSeconds = 20 * 60L
                eyeRestDurationSeconds = 20L
                waterIntervalSeconds = 45 * 60L
                waterBreakDurationSeconds = 5 * 60L
                waterRemindLaterSeconds = 5 * 60L
            }
            AppLogger.i(LogFeature.TimerEngine, "Debug Test Mode toggled: $value (Eye: ${eyeStudyIntervalSeconds}s, Water: ${waterIntervalSeconds}s)")
        }

    // Current session properties
    private var timerId: String = "TIMER-1"
    private var sessionUid: String = ""
    private var currentState: TimerState = TimerState.IDLE
    private var currentType: TimerType = TimerType.STUDY_SESSION

    private var subjectId: Long = 1
    private var topicId: Long = 1
    private var subjectName: String = "English Literature"
    private var topicName: String = "Romantic Poetry"
    private var sessionNotes: String = ""

    private var plannedDurationMillis: Long = 50 * 60 * 1000L
    private var segmentTotalDurationMillis: Long = 50 * 60 * 1000L

    // Timestamp & Elapsed trackers
    private var sessionStartWallTimestamp: Long = 0L
    private var segmentStartMonotonicMillis: Long = 0L
    private var segmentPausedMonotonicMillis: Long = 0L

    // Separate accumulators for distinct buckets
    private var accumulatedProductiveStudyMillis: Long = 0L
    private var accumulatedEyeRestMillis: Long = 0L
    private var accumulatedWaterBreakMillis: Long = 0L
    private var accumulatedOtherBreakMillis: Long = 0L

    // Trackers for Eye & Water triggers based on productive study elapsed
    private var productiveStudyMillisAtLastEyeRest: Long = 0L
    private var productiveStudyMillisAtLastWaterPrompt: Long = 0L

    private var interruptionCount: Int = 0
    private var currentPomodoroCycle: Int = 1
    private var totalPomodoroCycles: Int = 4
    private var pomodoroShortBreakMillis: Long = 5 * 60 * 1000L
    private var pomodoroLongBreakMillis: Long = 15 * 60 * 1000L
    private var pomodoroLongBreakFrequency: Int = 4

    private val lapsList = mutableListOf<LapRecord>()
    private var lastLapTotalMillis: Long = 0L

    // Listeners / Callback hooks
    var onSessionCompleted: ((TimerSnapshot) -> Unit)? = null
    var onEyeRestTriggered: ((TimerSnapshot) -> Unit)? = null
    var onWaterBreakTriggered: ((TimerSnapshot) -> Unit)? = null
    var onStateChangedCallback: ((TimerSnapshot) -> Unit)? = null

    private val _snapshot = MutableStateFlow(TimerSnapshot())
    val snapshot: StateFlow<TimerSnapshot> = _snapshot.asStateFlow()

    init {
        updateSnapshot()
    }

    @Synchronized
    fun startStudySession(
        subjectId: Long,
        topicId: Long,
        subjectName: String,
        topicName: String,
        type: TimerType = TimerType.STUDY_SESSION,
        durationMinutes: Int = 50,
        notes: String = "",
        pomodoroSettings: PomodoroSettings? = null
    ) {
        timerId = "TIMER-${System.currentTimeMillis() % 10000}"
        sessionUid = "SESSION-${UUID.randomUUID().toString().take(8).uppercase()}"
        currentState = TimerState.STUDYING
        currentType = type
        this.subjectId = subjectId
        this.topicId = topicId
        this.subjectName = subjectName
        this.topicName = topicName
        this.sessionNotes = notes

        plannedDurationMillis = if (type == TimerType.STOPWATCH) 0L else durationMinutes * 60 * 1000L
        segmentTotalDurationMillis = plannedDurationMillis

        pomodoroSettings?.let {
            currentPomodoroCycle = 1
            totalPomodoroCycles = it.cycles
            pomodoroShortBreakMillis = it.shortBreakMinutes * 60 * 1000L
            pomodoroLongBreakMillis = it.longBreakMinutes * 60 * 1000L
            pomodoroLongBreakFrequency = it.longBreakFrequency
            plannedDurationMillis = it.studyMinutes * 60 * 1000L
            segmentTotalDurationMillis = plannedDurationMillis
        }

        sessionStartWallTimestamp = System.currentTimeMillis()
        segmentStartMonotonicMillis = SystemClock.elapsedRealtime()
        segmentPausedMonotonicMillis = 0L

        accumulatedProductiveStudyMillis = 0L
        accumulatedEyeRestMillis = 0L
        accumulatedWaterBreakMillis = 0L
        accumulatedOtherBreakMillis = 0L

        productiveStudyMillisAtLastEyeRest = 0L
        productiveStudyMillisAtLastWaterPrompt = 0L
        interruptionCount = 0
        lapsList.clear()
        lastLapTotalMillis = 0L

        AppLogger.i(
            LogFeature.TimerEngine,
            "Study session started: $type",
            "Duration: ${durationMinutes}m, Subject: $subjectName, Topic: $topicName",
            currentState.name,
            sessionUid,
            timerId,
            subjectName,
            topicName
        )

        startTicker()
        updateSnapshot()
        notifyStateChange()
    }

    @Synchronized
    fun pause() {
        if (currentState != TimerState.STUDYING && !currentState.isBreak) return

        val now = SystemClock.elapsedRealtime()
        commitActiveElapsed(now)
        segmentPausedMonotonicMillis = now
        val previousState = currentState
        currentState = TimerState.PAUSED
        interruptionCount++
        stopTicker()

        AppLogger.i(
            LogFeature.TimerEngine,
            "Timer paused from $previousState",
            "Interruptions: $interruptionCount",
            currentState.name,
            sessionUid,
            timerId,
            subjectName,
            topicName
        )

        updateSnapshot()
        notifyStateChange()
    }

    @Synchronized
    fun resume() {
        if (currentState != TimerState.PAUSED) return

        val now = SystemClock.elapsedRealtime()
        segmentStartMonotonicMillis = now
        segmentPausedMonotonicMillis = 0L
        currentState = TimerState.STUDYING
        segmentTotalDurationMillis = plannedDurationMillis

        AppLogger.i(
            LogFeature.TimerEngine,
            "Timer resumed",
            "Resumed studying segment",
            currentState.name,
            sessionUid,
            timerId,
            subjectName,
            topicName
        )

        startTicker()
        updateSnapshot()
        notifyStateChange()
    }

    @Synchronized
    fun stop() {
        if (currentState == TimerState.IDLE || currentState == TimerState.STOPPED) return

        val now = SystemClock.elapsedRealtime()
        commitActiveElapsed(now)
        currentState = TimerState.STOPPED
        stopTicker()
        breakQueue.clear()

        AppLogger.i(
            LogFeature.TimerEngine,
            "Timer stopped manually",
            "Productive study: ${accumulatedProductiveStudyMillis / 1000}s",
            currentState.name,
            sessionUid,
            timerId,
            subjectName,
            topicName
        )

        val finalSnapshot = calculateSnapshot(now)
        _snapshot.value = finalSnapshot
        onSessionCompleted?.invoke(finalSnapshot)
        notifyStateChange()
    }

    @Synchronized
    fun setPlannedDuration(minutes: Int) {
        plannedDurationMillis = (minutes * 60 * 1000L).coerceAtLeast(60 * 1000L)
        if (currentState == TimerState.STUDYING || currentState == TimerState.PAUSED || currentState.isBreak) {
            if (currentState == TimerState.STUDYING || currentState == TimerState.PAUSED) {
                segmentTotalDurationMillis = plannedDurationMillis
            }
        } else if (currentState == TimerState.IDLE) {
            segmentTotalDurationMillis = plannedDurationMillis
        }
        updateSnapshot()
        notifyStateChange()
    }

    @Synchronized
    fun reset() {
        stopTicker()
        currentState = TimerState.IDLE
        accumulatedProductiveStudyMillis = 0L
        accumulatedEyeRestMillis = 0L
        accumulatedWaterBreakMillis = 0L
        accumulatedOtherBreakMillis = 0L
        breakQueue.clear()
        lapsList.clear()
        segmentStartMonotonicMillis = 0L
        segmentPausedMonotonicMillis = 0L

        AppLogger.i(
            LogFeature.TimerEngine,
            "Timer reset to IDLE",
            null,
            currentState.name,
            sessionUid,
            timerId,
            subjectName,
            topicName
        )

        updateSnapshot()
        notifyStateChange()
    }

    @Synchronized
    fun recordLap() {
        if (currentType != TimerType.STOPWATCH || currentState != TimerState.STUDYING) return

        val now = SystemClock.elapsedRealtime()
        val currentTotalMillis = accumulatedProductiveStudyMillis + (now - segmentStartMonotonicMillis)
        val lapDuration = currentTotalMillis - lastLapTotalMillis
        lastLapTotalMillis = currentTotalMillis

        val lap = LapRecord(
            lapNumber = lapsList.size + 1,
            lapDurationMillis = lapDuration,
            totalDurationMillis = currentTotalMillis
        )
        lapsList.add(0, lap)

        AppLogger.i(
            LogFeature.TimerEngine,
            "Stopwatch Lap ${lap.lapNumber}",
            "Lap: ${lap.formatDuration()}, Total: ${lap.formatTotal()}",
            currentState.name,
            sessionUid,
            timerId,
            subjectName,
            topicName
        )

        updateSnapshot()
    }

    // 20-20-20 EYE CARE METHODS
    @Synchronized
    fun triggerEyeRestAlert() {
        if (currentState == TimerState.WATER_BREAK) {
            if (!breakQueue.contains(BreakType.EYE_REST)) {
                breakQueue.add(BreakType.EYE_REST)
            }
            return
        }
        val now = SystemClock.elapsedRealtime()
        commitActiveElapsed(now)
        currentState = TimerState.EYE_REST
        segmentStartMonotonicMillis = now
        segmentTotalDurationMillis = eyeRestDurationSeconds * 1000L
        productiveStudyMillisAtLastEyeRest = accumulatedProductiveStudyMillis

        AppLogger.i(
            LogFeature.EyeCare,
            "Eye rest alert triggered (${eyeRestDurationSeconds}s)",
            "Automatic pause of study timer",
            currentState.name,
            sessionUid,
            timerId,
            subjectName,
            topicName
        )

        updateSnapshot()
        val snap = _snapshot.value
        onEyeRestTriggered?.invoke(snap)
        notifyStateChange()
    }

    @Synchronized
    fun completeEyeRest(autoResumed: Boolean = false) {
        if (currentState != TimerState.EYE_REST) return

        val now = SystemClock.elapsedRealtime()
        commitActiveElapsed(now)
        productiveStudyMillisAtLastEyeRest = accumulatedProductiveStudyMillis

        AppLogger.i(
            LogFeature.EyeCare,
            "Eye rest completed",
            "Auto resumed: $autoResumed",
            currentState.name,
            sessionUid,
            timerId,
            subjectName,
            topicName
        )

        if (breakQueue.isNotEmpty()) {
            val nextBreak = breakQueue.removeFirst()
            if (nextBreak == BreakType.WATER_BREAK) {
                triggerWaterReminder()
                return
            }
        }

        if (eyeAutoResumeEnabled || autoResumed) {
            currentState = TimerState.STUDYING
            segmentStartMonotonicMillis = now
            segmentTotalDurationMillis = plannedDurationMillis
            AppLogger.i(LogFeature.TimerEngine, "State changed: EYE_REST -> STUDYING", null, currentState.name, sessionUid, timerId)
        } else {
            currentState = TimerState.PAUSED
            segmentPausedMonotonicMillis = now
        }

        updateSnapshot()
        notifyStateChange()
    }

    @Synchronized
    fun skipEyeRest() {
        if (currentState != TimerState.EYE_REST) return

        val now = SystemClock.elapsedRealtime()
        commitActiveElapsed(now)
        productiveStudyMillisAtLastEyeRest = accumulatedProductiveStudyMillis

        AppLogger.i(
            LogFeature.EyeCare,
            "Eye rest skipped by user",
            "Study timer resumed",
            currentState.name,
            sessionUid,
            timerId,
            subjectName,
            topicName
        )

        if (breakQueue.isNotEmpty()) {
            val nextBreak = breakQueue.removeFirst()
            if (nextBreak == BreakType.WATER_BREAK) {
                triggerWaterReminder()
                return
            }
        }

        currentState = TimerState.STUDYING
        segmentStartMonotonicMillis = now
        segmentTotalDurationMillis = plannedDurationMillis
        updateSnapshot()
        notifyStateChange()
    }

    // HYDRATION WATER BREAK METHODS
    @Synchronized
    fun triggerWaterReminder() {
        if (currentState == TimerState.EYE_REST) {
            if (!breakQueue.contains(BreakType.WATER_BREAK)) {
                breakQueue.add(BreakType.WATER_BREAK)
            }
            return
        }
        val now = SystemClock.elapsedRealtime()
        commitActiveElapsed(now)
        currentState = TimerState.WATER_BREAK
        segmentStartMonotonicMillis = now
        segmentTotalDurationMillis = waterBreakDurationSeconds * 1000L
        productiveStudyMillisAtLastWaterPrompt = accumulatedProductiveStudyMillis

        AppLogger.i(
            LogFeature.Hydration,
            "Hydration reminder triggered (${waterBreakDurationSeconds}s)",
            "Automatic pause of study timer",
            currentState.name,
            sessionUid,
            timerId,
            subjectName,
            topicName
        )

        updateSnapshot()
        val snap = _snapshot.value
        onWaterBreakTriggered?.invoke(snap)
        notifyStateChange()
    }

    @Synchronized
    fun completeWaterBreak(autoResumed: Boolean = false) {
        if (currentState != TimerState.WATER_BREAK) return

        val now = SystemClock.elapsedRealtime()
        commitActiveElapsed(now)
        productiveStudyMillisAtLastWaterPrompt = accumulatedProductiveStudyMillis

        AppLogger.i(
            LogFeature.Hydration,
            "Water break completed (${waterBreakDurationSeconds}s)",
            "Auto resumed: $autoResumed",
            currentState.name,
            sessionUid,
            timerId,
            subjectName,
            topicName
        )

        if (breakQueue.isNotEmpty()) {
            val nextBreak = breakQueue.removeFirst()
            if (nextBreak == BreakType.EYE_REST) {
                triggerEyeRestAlert()
                return
            }
        }

        if (waterAutoResumeEnabled || autoResumed) {
            currentState = TimerState.STUDYING
            segmentStartMonotonicMillis = now
            segmentTotalDurationMillis = plannedDurationMillis
            AppLogger.i(LogFeature.TimerEngine, "State changed: WATER_BREAK -> STUDYING", null, currentState.name, sessionUid, timerId)
        } else {
            currentState = TimerState.PAUSED
            segmentPausedMonotonicMillis = now
        }

        updateSnapshot()
        notifyStateChange()
    }

    @Synchronized
    fun confirmDrankWater() {
        if (currentState != TimerState.WATER_BREAK) return
        completeWaterBreak(autoResumed = true)
    }

    @Synchronized
    fun remindWaterLater() {
        val now = SystemClock.elapsedRealtime()
        commitActiveElapsed(now)
        // Adjust tracker so next reminder occurs in waterRemindLaterSeconds
        val snoozeMillis = waterRemindLaterSeconds * 1000L
        val intervalMillis = waterIntervalSeconds * 1000L
        productiveStudyMillisAtLastWaterPrompt = (accumulatedProductiveStudyMillis - (intervalMillis - snoozeMillis)).coerceAtLeast(0)

        currentState = TimerState.STUDYING
        segmentStartMonotonicMillis = now
        segmentTotalDurationMillis = plannedDurationMillis

        AppLogger.i(
            LogFeature.Hydration,
            "Water reminder snoozed: [REMIND LATER] (${waterRemindLaterSeconds}s)",
            null,
            currentState.name,
            sessionUid,
            timerId,
            subjectName,
            topicName
        )

        updateSnapshot()
        notifyStateChange()
    }

    @Synchronized
    fun skipWaterReminder() {
        if (currentState != TimerState.WATER_BREAK) return
        val now = SystemClock.elapsedRealtime()
        commitActiveElapsed(now)
        productiveStudyMillisAtLastWaterPrompt = accumulatedProductiveStudyMillis

        AppLogger.i(
            LogFeature.Hydration,
            "Water reminder skipped: [SKIP]",
            null,
            currentState.name,
            sessionUid,
            timerId,
            subjectName,
            topicName
        )

        if (breakQueue.isNotEmpty()) {
            val nextBreak = breakQueue.removeFirst()
            if (nextBreak == BreakType.EYE_REST) {
                triggerEyeRestAlert()
                return
            }
        }

        currentState = TimerState.STUDYING
        segmentStartMonotonicMillis = now
        segmentTotalDurationMillis = plannedDurationMillis
        updateSnapshot()
        notifyStateChange()
    }

    // BREAK ENGINE METHODS
    @Synchronized
    fun startBreak(breakType: BreakType, durationMinutes: Int) {
        val now = SystemClock.elapsedRealtime()
        commitActiveElapsed(now)

        currentState = when (breakType) {
            BreakType.SHORT_BREAK -> TimerState.SHORT_BREAK
            BreakType.LONG_BREAK -> TimerState.LONG_BREAK
            BreakType.EYE_REST -> TimerState.EYE_REST
            BreakType.WATER_BREAK -> TimerState.WATER_BREAK
            BreakType.CUSTOM_BREAK -> TimerState.SHORT_BREAK
        }
        segmentStartMonotonicMillis = now
        segmentTotalDurationMillis = durationMinutes * 60 * 1000L

        AppLogger.i(
            LogFeature.BreakEngine,
            "Break started: ${breakType.displayName} ($durationMinutes min)",
            null,
            currentState.name,
            sessionUid,
            timerId,
            subjectName,
            topicName
        )

        updateSnapshot()
        notifyStateChange()
    }

    @Synchronized
    fun skipOrEndBreak() {
        if (!currentState.isBreak) return

        val now = SystemClock.elapsedRealtime()
        commitActiveElapsed(now)
        currentState = TimerState.STUDYING
        segmentStartMonotonicMillis = now
        segmentTotalDurationMillis = plannedDurationMillis

        AppLogger.i(
            LogFeature.BreakEngine,
            "Break ended or skipped -> resuming study",
            null,
            currentState.name,
            sessionUid,
            timerId,
            subjectName,
            topicName
        )

        updateSnapshot()
        notifyStateChange()
    }

    private fun commitActiveElapsed(now: Long) {
        if (segmentStartMonotonicMillis <= 0) return
        val elapsedThisRun = (now - segmentStartMonotonicMillis).coerceAtLeast(0)

        when (currentState) {
            TimerState.STUDYING -> accumulatedProductiveStudyMillis += elapsedThisRun
            TimerState.EYE_REST -> accumulatedEyeRestMillis += elapsedThisRun
            TimerState.WATER_BREAK -> accumulatedWaterBreakMillis += elapsedThisRun
            TimerState.SHORT_BREAK, TimerState.LONG_BREAK -> accumulatedOtherBreakMillis += elapsedThisRun
            else -> { /* No bucket */ }
        }
        segmentStartMonotonicMillis = now
    }

    @Synchronized
    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000)
                handleTick()
            }
        }
    }

    @Synchronized
    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    @Synchronized
    private fun handleTick() {
        if (currentState == TimerState.IDLE || currentState == TimerState.PAUSED || currentState == TimerState.STOPPED || currentState == TimerState.COMPLETED) {
            return
        }

        val now = SystemClock.elapsedRealtime()
        val currentSegmentElapsed = (now - segmentStartMonotonicMillis).coerceAtLeast(0)

        // Handle Active STUDYING State checks
        if (currentState == TimerState.STUDYING) {
            val totalProductive = accumulatedProductiveStudyMillis + currentSegmentElapsed

            // 1. Check countdown completion based on planned duration
            if (currentType != TimerType.STOPWATCH && plannedDurationMillis > 0) {
                if (totalProductive >= plannedDurationMillis) {
                    commitActiveElapsed(now)
                    handleSegmentCompletion()
                    return
                }
            }

            // 2. Check 20-20-20 Eye Care Trigger (every 20 minutes of study)
            if (eyeCareEnabled) {
                val eyeIntervalMillis = eyeStudyIntervalSeconds * 1000L
                if (totalProductive - productiveStudyMillisAtLastEyeRest >= eyeIntervalMillis) {
                    triggerEyeRestAlert()
                    return
                }
            }

            // 3. Check Water Reminder Trigger (every 45 minutes of study)
            if (waterReminderEnabled) {
                val waterIntervalMillis = waterIntervalSeconds * 1000L
                if (totalProductive - productiveStudyMillisAtLastWaterPrompt >= waterIntervalMillis) {
                    triggerWaterReminder()
                    return
                }
            }
        } else if (currentState == TimerState.EYE_REST) {
            // Check Eye rest countdown (20 seconds)
            val eyeRemaining = (eyeRestDurationSeconds * 1000L - currentSegmentElapsed).coerceAtLeast(0)
            if (eyeRemaining <= 0) {
                completeEyeRest(autoResumed = true)
                return
            }
        } else if (currentState == TimerState.WATER_BREAK) {
            // Check Water break countdown (5 minutes)
            val waterRemaining = (waterBreakDurationSeconds * 1000L - currentSegmentElapsed).coerceAtLeast(0)
            if (waterRemaining <= 0) {
                completeWaterBreak(autoResumed = true)
                return
            }
        } else if (currentState == TimerState.SHORT_BREAK || currentState == TimerState.LONG_BREAK) {
            // Check Break countdown
            val breakRemaining = (segmentTotalDurationMillis - currentSegmentElapsed).coerceAtLeast(0)
            if (breakRemaining <= 0) {
                commitActiveElapsed(now)
                handleBreakCompleted()
                return
            }
        }

        updateSnapshot()
    }

    private fun handleSegmentCompletion() {
        when (currentType) {
            TimerType.POMODORO -> {
                // Transition to Short or Long Break
                val isLongBreak = currentPomodoroCycle % pomodoroLongBreakFrequency == 0
                currentState = if (isLongBreak) TimerState.LONG_BREAK else TimerState.SHORT_BREAK
                val breakDuration = if (isLongBreak) pomodoroLongBreakMillis else pomodoroShortBreakMillis
                segmentTotalDurationMillis = breakDuration
                segmentStartMonotonicMillis = SystemClock.elapsedRealtime()

                AppLogger.i(
                    LogFeature.Pomodoro,
                    "Pomodoro study segment finished -> entering ${currentState.name}",
                    "Cycle $currentPomodoroCycle of $totalPomodoroCycles",
                    currentState.name,
                    sessionUid,
                    timerId,
                    subjectName,
                    topicName
                )
                updateSnapshot()
                notifyStateChange()
            }
            else -> {
                currentState = TimerState.COMPLETED
                stopTicker()
                AppLogger.i(
                    LogFeature.TimerEngine,
                    "Study session completed!",
                    "Productive study: ${accumulatedProductiveStudyMillis / 1000}s",
                    currentState.name,
                    sessionUid,
                    timerId,
                    subjectName,
                    topicName
                )
                val finalSnap = calculateSnapshot(SystemClock.elapsedRealtime())
                _snapshot.value = finalSnap
                onSessionCompleted?.invoke(finalSnap)
                notifyStateChange()
            }
        }
    }

    private fun handleBreakCompleted() {
        if (currentType == TimerType.POMODORO) {
            currentPomodoroCycle++
            if (currentPomodoroCycle > totalPomodoroCycles) {
                currentState = TimerState.COMPLETED
                stopTicker()
                AppLogger.i(LogFeature.Pomodoro, "All $totalPomodoroCycles Pomodoro cycles completed!")
                val finalSnap = calculateSnapshot(SystemClock.elapsedRealtime())
                _snapshot.value = finalSnap
                onSessionCompleted?.invoke(finalSnap)
                notifyStateChange()
                return
            }
            // Resume Pomodoro study cycle
            currentState = TimerState.STUDYING
            segmentTotalDurationMillis = plannedDurationMillis
            segmentStartMonotonicMillis = SystemClock.elapsedRealtime()
            AppLogger.i(LogFeature.Pomodoro, "Starting Pomodoro cycle $currentPomodoroCycle of $totalPomodoroCycles")
            updateSnapshot()
            notifyStateChange()
        } else {
            currentState = TimerState.STUDYING
            segmentStartMonotonicMillis = SystemClock.elapsedRealtime()
            updateSnapshot()
            notifyStateChange()
        }
    }

    private fun updateSnapshot() {
        val now = SystemClock.elapsedRealtime()
        _snapshot.value = calculateSnapshot(now)
    }

    private fun calculateSnapshot(now: Long): TimerSnapshot {
        val activeSegmentElapsed = if (currentState == TimerState.STUDYING || currentState.isBreak) {
            (now - segmentStartMonotonicMillis).coerceAtLeast(0)
        } else 0L

        val currentProductive = if (currentState == TimerState.STUDYING) {
            accumulatedProductiveStudyMillis + activeSegmentElapsed
        } else {
            accumulatedProductiveStudyMillis
        }

        val currentEyeRest = if (currentState == TimerState.EYE_REST) {
            accumulatedEyeRestMillis + activeSegmentElapsed
        } else accumulatedEyeRestMillis

        val currentWaterBreak = if (currentState == TimerState.WATER_BREAK) {
            accumulatedWaterBreakMillis + activeSegmentElapsed
        } else accumulatedWaterBreakMillis

        val currentOtherBreak = if (currentState == TimerState.SHORT_BREAK || currentState == TimerState.LONG_BREAK) {
            accumulatedOtherBreakMillis + activeSegmentElapsed
        } else accumulatedOtherBreakMillis

        val remainingStudyMillis = if (currentType == TimerType.STOPWATCH) {
            0L
        } else {
            (plannedDurationMillis - currentProductive).coerceAtLeast(0)
        }

        val segmentRemaining = when (currentState) {
            TimerState.EYE_REST -> (eyeRestDurationSeconds * 1000L - activeSegmentElapsed).coerceAtLeast(0)
            TimerState.WATER_BREAK -> (waterBreakDurationSeconds * 1000L - activeSegmentElapsed).coerceAtLeast(0)
            TimerState.SHORT_BREAK, TimerState.LONG_BREAK -> (segmentTotalDurationMillis - activeSegmentElapsed).coerceAtLeast(0)
            else -> remainingStudyMillis
        }

        val eyeIntervalMillis = eyeStudyIntervalSeconds * 1000L
        val productiveSinceEye = (currentProductive - productiveStudyMillisAtLastEyeRest).coerceAtLeast(0)
        val nextEyeSeconds = if (currentState == TimerState.EYE_REST) {
            (segmentRemaining / 1000).coerceAtLeast(0)
        } else {
            ((eyeIntervalMillis - productiveSinceEye) / 1000).coerceAtLeast(0)
        }

        val waterIntervalMillis = waterIntervalSeconds * 1000L
        val productiveSinceWater = (currentProductive - productiveStudyMillisAtLastWaterPrompt).coerceAtLeast(0)
        val nextWaterSeconds = if (currentState == TimerState.WATER_BREAK) {
            (segmentRemaining / 1000).coerceAtLeast(0)
        } else {
            ((waterIntervalMillis - productiveSinceWater) / 1000).coerceAtLeast(0)
        }

        return TimerSnapshot(
            timerId = timerId,
            sessionUid = sessionUid,
            state = currentState,
            timerType = currentType,
            subjectId = subjectId,
            topicId = topicId,
            subjectName = subjectName,
            topicName = topicName,
            plannedDurationMillis = plannedDurationMillis,
            currentTimerDurationMillis = segmentTotalDurationMillis,
            elapsedProductiveMillis = currentProductive,
            elapsedEyeRestMillis = currentEyeRest,
            elapsedWaterBreakMillis = currentWaterBreak,
            elapsedOtherBreakMillis = currentOtherBreak,
            currentSegmentRemainingMillis = segmentRemaining,
            currentSegmentElapsedMillis = if (currentType == TimerType.STOPWATCH) currentProductive else activeSegmentElapsed,
            nextEyeRestCountdownSeconds = nextEyeSeconds,
            eyeRestDurationSeconds = eyeRestDurationSeconds,
            nextWaterCountdownSeconds = nextWaterSeconds,
            currentPomodoroCycle = currentPomodoroCycle,
            totalPomodoroCycles = totalPomodoroCycles,
            interruptionCount = interruptionCount,
            laps = lapsList.toList(),
            notes = sessionNotes,
            startTimestampMillis = sessionStartWallTimestamp,
            debugModeActive = debugModeActive
        )
    }

    private fun notifyStateChange() {
        onStateChangedCallback?.invoke(_snapshot.value)
    }
}

data class PomodoroSettings(
    val studyMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val cycles: Int = 4,
    val longBreakFrequency: Int = 4
)
