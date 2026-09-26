package com.yokos.bb10launcher.hub

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Colour stripe family for a Hub entry, derived from the notification's category. */
enum class HubCategory {
    Message, Social, Call, Alarm, Error, Progress, Default;

    companion object {
        /** Maps `Notification.CATEGORY_*` values (plain strings, so this stays JVM-testable). */
        fun fromNotificationCategory(category: String?): HubCategory = when (category) {
            "msg", "email" -> Message
            "social" -> Social
            "call", "missed_call" -> Call
            "alarm", "reminder", "event" -> Alarm
            "err" -> Error
            "progress" -> Progress
            else -> Default
        }
    }
}

/** One message in the Hub. Plain data: the listener keeps the live notification for actions. */
data class HubItem(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val category: HubCategory,
    val canReply: Boolean,
    val canOpen: Boolean,
)

/** One app in the Hub's account rail. */
data class HubAccount(
    val packageName: String,
    val label: String,
    val unread: Int,
    val total: Int,
)

data class HubState(
    val connected: Boolean = false,
    val items: List<HubItem> = emptyList(),
    val readKeys: Set<String> = emptySet(),
) {
    fun isUnread(item: HubItem) = item.key !in readKeys

    val unreadCount: Int get() = items.count(::isUnread)

    fun filtered(packageName: String?): List<HubItem> =
        if (packageName == null) items else items.filter { it.packageName == packageName }

    /** Apps with entries, most recently active first. */
    fun accounts(): List<HubAccount> = items
        .groupBy { it.packageName }
        .map { (pkg, entries) ->
            HubAccount(pkg, entries.first().appLabel, entries.count(::isUnread), entries.size) to
                entries.maxOf { it.postTime }
        }
        .sortedByDescending { it.second }
        .map { it.first }

    fun search(query: String): List<HubItem> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return items.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.text.contains(q, ignoreCase = true) ||
                it.appLabel.contains(q, ignoreCase = true)
        }
    }
}

sealed interface DayBucket {
    data object Today : DayBucket
    data object Yesterday : DayBucket
    data class On(val date: LocalDate) : DayBucket
}

/** Splits time-sorted items into Today / Yesterday / dated sections, keeping their order. */
fun groupByDay(items: List<HubItem>, now: Long, zone: ZoneId): List<Pair<DayBucket, List<HubItem>>> {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return items
        .groupBy { item ->
            when (val date = Instant.ofEpochMilli(item.postTime).atZone(zone).toLocalDate()) {
                today -> DayBucket.Today
                today.minusDays(1) -> DayBucket.Yesterday
                else -> DayBucket.On(date)
            }
        }
        .toList()
}
