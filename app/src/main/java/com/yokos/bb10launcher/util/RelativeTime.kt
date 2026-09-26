package com.yokos.bb10launcher.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yokos.bb10launcher.R

@Composable
fun ageText(elapsedMillis: Long): String = when (val age = Age.of(elapsedMillis)) {
    Age.Now -> stringResource(R.string.time_now)
    is Age.Minutes -> stringResource(R.string.time_minutes_ago, age.value)
    is Age.Hours -> stringResource(R.string.time_hours_ago, age.value)
    is Age.Days -> stringResource(R.string.time_days_ago, age.value)
}
