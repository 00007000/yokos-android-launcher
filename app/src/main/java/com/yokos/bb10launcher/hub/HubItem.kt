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

        /** Uses the notification's category, falling back to well-known apps when it has none. */
        fun classify(category: String?, packageName: String): HubCategory {
            val byCategory = fromNotificationCategory(category)
            if (byCategory != Default) return byCategory
            return when (packageName) {
                in MESSAGE_APPS -> Message
                in SOCIAL_APPS -> Social
                in CALL_APPS -> Call
                else -> Default
            }
        }

        private val MESSAGE_APPS = setOf(
            "com.google.android.gm", "com.microsoft.office.outlook", "de.tutao.tutanota",
            "com.yahoo.mobile.client.android.mail", "com.android.mms", "com.google.android.apps.messaging",
            "com.samsung.android.messaging", "com.textra", "org.thoughtcrime.securesms",
            "com.whatsapp", "org.telegram.messenger",
        )
        private val SOCIAL_APPS = setOf(
            "com.twitter.android", "com.instagram.android", "com.facebook.katana", "com.facebook.orca",
            "com.linkedin.android", "com.reddit.frontpage", "com.snapchat.android",
        )
        private val CALL_APPS = setOf(
            "com.android.phone", "com.android.server.telecom", "com.google.android.dialer",
            "com.samsung.android.incallui", "com.samsung.android.dialer",
        )
    }
}

/** A notification as the listener sees it, before it becomes a Hub history entry. */
data class PostedNotification(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val category: HubCategory,
    val hasReply: Boolean,
)

/**
 * One entry in the Hub history. Entries outlive the notification that created them: [active]
 * says whether it is still in the status bar, which is what reply and snooze need.
 */
data class HubItem(
    val id: Long,
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val category: HubCategory,
    val hasReply: Boolean,
    val active: Boolean,
    val read: Boolean,
) {
    val canReply: Boolean get() = active && hasReply

    /** Snoozing needs the live notification. */
    val canSnooze: Boolean get() = active
}

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
) {
    fun isUnread(item: HubItem) = !item.read

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
