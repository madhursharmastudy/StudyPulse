package com.example.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DeskClockDisplay(
    use24Hour: Boolean = false,
    showSeconds: Boolean = false,
    clockStyle: String = "Digital",
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance()
            delay(500)
        }
    }

    val hourFormat = if (use24Hour) "HH" else "hh"
    val hoursStr = SimpleDateFormat(hourFormat, Locale.getDefault()).format(currentTime.time)
    val minutesStr = SimpleDateFormat("mm", Locale.getDefault()).format(currentTime.time)
    val secondsStr = SimpleDateFormat("ss", Locale.getDefault()).format(currentTime.time)
    val amPmStr = if (!use24Hour) SimpleDateFormat("a", Locale.getDefault()).format(currentTime.time).uppercase() else ""
    
    val dateDayStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(currentTime.time).uppercase()
    val dayOfWeekStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(currentTime.time).uppercase()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Date row: e.g. "09 SEP 2026"
        Text(
            text = dateDayStr,
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Medium
            ),
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        // Day row: e.g. "WEDNESDAY" in warm gold
        Text(
            text = dayOfWeekStr,
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Bold
            ),
            color = StudyAccent,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Hero Clock row: e.g. "11:13 AM"
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$hoursStr:$minutesStr",
                fontSize = 82.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp,
                color = TextPrimary,
                lineHeight = 84.sp
            )

            if (showSeconds) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = ":$secondsStr",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (amPmStr.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = amPmStr,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}
