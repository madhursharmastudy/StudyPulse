package com.example.core.timer

enum class TimerState {
    IDLE,
    STUDYING,
    PAUSED,
    EYE_REST,
    WATER_BREAK,
    SHORT_BREAK,
    LONG_BREAK,
    COMPLETED,
    STOPPED;

    val isBreak: Boolean
        get() = this == EYE_REST || this == WATER_BREAK || this == SHORT_BREAK || this == LONG_BREAK

    val isActive: Boolean
        get() = this == STUDYING || isBreak || this == PAUSED
}
