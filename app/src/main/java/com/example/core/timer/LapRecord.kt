package com.example.core.timer

data class LapRecord(
    val lapNumber: Int,
    val lapDurationMillis: Long,
    val totalDurationMillis: Long
) {
    fun formatDuration(): String = formatMillis(lapDurationMillis)
    fun formatTotal(): String = formatMillis(totalDurationMillis)

    companion object {
        fun formatMillis(millis: Long): String {
            val totalSeconds = millis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            val hundredths = (millis % 1000) / 10
            return if (hours > 0) {
                String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, hundredths)
            } else {
                String.format("%02d:%02d.%02d", minutes, seconds, hundredths)
            }
        }
    }
}
