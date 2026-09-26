package com.yokos.bb10launcher.util

/** How long ago something happened, bucketed the way BB10 showed it. */
sealed interface Age {
    data object Now : Age
    data class Minutes(val value: Long) : Age
    data class Hours(val value: Long) : Age
    data class Days(val value: Long) : Age

    companion object {
        fun of(elapsedMillis: Long): Age {
            val minutes = elapsedMillis.coerceAtLeast(0) / 60_000
            return when {
                minutes < 1 -> Now
                minutes < 60 -> Minutes(minutes)
                minutes < 60 * 24 -> Hours(minutes / 60)
                else -> Days(minutes / (60 * 24))
            }
        }
    }
}
