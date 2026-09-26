package com.yokos.bb10launcher.frames

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/** Reads app foreground/background transitions from UsageStatsManager (needs usage access). */
class RecentAppsSource(context: Context) {
    private val usageStats = context.getSystemService(UsageStatsManager::class.java)

    fun load(now: Long = System.currentTimeMillis()): List<UsageEvent> {
        val manager = usageStats ?: return emptyList()
        val events = manager.queryEvents(now - WINDOW_MILLIS, now) ?: return emptyList()
        val result = ArrayList<UsageEvent>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            @Suppress("DEPRECATION") // ACTIVITY_RESUMED/PAUSED share these values but need API 29.
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND
            ) {
                result += UsageEvent(event.packageName, event.timeStamp)
            }
        }
        return result
    }

    companion object {
        const val WINDOW_MILLIS = 24 * 60 * 60 * 1000L
    }
}
