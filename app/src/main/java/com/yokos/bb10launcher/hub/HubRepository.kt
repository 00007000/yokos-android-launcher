package com.yokos.bb10launcher.hub

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Acts on the live notification behind a Hub entry. Implemented by the connected listener. */
interface HubController {
    fun open(key: String): Boolean
    fun dismiss(key: String)
    fun snooze(key: String, durationMillis: Long)
    fun reply(key: String, text: String): Boolean
}

/** Where the Hub history is kept. Writes may be applied asynchronously. */
interface HubStore {
    fun loadAll(): List<HubItem>
    fun upsert(items: Collection<HubItem>)
    fun delete(ids: Collection<Long>)
}

/**
 * The Hub history: every notification the listener has seen, kept after it leaves the status bar
 * and across restarts. The listener feeds it; the launcher and the peek overlay observe [state].
 */
class HubRepository(
    private val store: HubStore,
    private val openApp: (packageName: String) -> Boolean,
    private val clock: () -> Long = System::currentTimeMillis,
    private val maxItems: Int = MAX_ITEMS,
    private val retentionMillis: Long = RETENTION_MILLIS,
) {
    private val lock = Any()
    private var nextId: Long

    // Nothing is in the status bar until the listener (re)connects and reports it.
    private val _state = MutableStateFlow(
        HubState(items = store.loadAll().map { it.copy(active = false) }.sortedByDescending { it.postTime }),
    )
    val state: StateFlow<HubState> = _state.asStateFlow()

    @Volatile
    var controller: HubController? = null

    init {
        nextId = (_state.value.items.maxOfOrNull { it.id } ?: 0L) + 1
        synchronized(lock) { prune() }
    }

    /** The listener connected: everything it reports is live, everything else has been cleared. */
    fun onConnected(posted: List<PostedNotification>) = synchronized(lock) {
        val cleared = _state.value.items.filter { it.active }.map { it.copy(active = false) }
        replace(cleared)
        posted.sortedBy { it.postTime }.forEach(::record)
        _state.value = _state.value.copy(connected = true)
        prune()
    }

    fun onDisconnected() = synchronized(lock) {
        _state.value = _state.value.copy(connected = false)
    }

    fun onPosted(notification: PostedNotification) = synchronized(lock) {
        record(notification)
        prune()
    }

    /** Left the status bar (dismissed elsewhere, or the app cancelled it). It stays in the Hub. */
    fun onRemoved(key: String) = synchronized(lock) {
        replace(_state.value.items.filter { it.key == key && it.active }.map { it.copy(active = false) })
    }

    fun markRead(ids: Collection<Long>) = setRead(ids.toSet(), read = true)

    fun markUnread(id: Long) = setRead(setOf(id), read = false)

    fun markAllRead(packageName: String?) = markRead(_state.value.filtered(packageName).map { it.id })

    /** Opens the conversation if the notification is still live, otherwise the app itself. */
    fun open(id: Long): Boolean {
        val item = find(id) ?: return false
        markRead(listOf(id))
        if (item.active && controller?.open(item.key) == true) return true
        return openApp(item.packageName)
    }

    /** Removes the entry from the Hub, and its notification from the status bar if still there. */
    fun delete(id: Long) = synchronized(lock) {
        val item = find(id) ?: return@synchronized
        if (item.active) controller?.dismiss(item.key)
        remove(listOf(id))
    }

    /** Clears the whole history, or one app's, without touching live notifications. */
    fun deleteAll(packageName: String?) = synchronized(lock) {
        remove(_state.value.filtered(packageName).filter { !it.active }.map { it.id })
    }

    /** Hides the entry until the notification comes back, when it returns as new and unread. */
    fun snooze(id: Long, durationMillis: Long) = synchronized(lock) {
        val item = find(id)?.takeIf { it.active } ?: return@synchronized
        controller?.snooze(item.key, durationMillis)
        remove(listOf(id))
    }

    fun reply(id: Long, text: String): Boolean {
        val item = find(id)?.takeIf { it.active } ?: return false
        val sent = controller?.reply(item.key, text) ?: false
        if (sent) markRead(listOf(id))
        return sent
    }

    private fun find(id: Long) = _state.value.items.firstOrNull { it.id == id }

    /**
     * Adds a posted notification to the history. A re-post with the same text (a silent update or
     * a reconnect) only refreshes the existing entry; new text is a new entry, so each message of a
     * conversation is kept.
     */
    private fun record(notification: PostedNotification) {
        val items = _state.value.items
        val latest = items.firstOrNull { it.key == notification.key }
        if (latest != null && latest.title == notification.title && latest.text == notification.text) {
            replace(listOf(latest.copy(active = true, hasReply = notification.hasReply, appLabel = notification.appLabel)))
            return
        }
        val superseded = items.filter { it.key == notification.key && it.active }.map { it.copy(active = false) }
        val entry = HubItem(
            id = nextId++,
            key = notification.key,
            packageName = notification.packageName,
            appLabel = notification.appLabel,
            title = notification.title,
            text = notification.text,
            postTime = notification.postTime,
            category = notification.category,
            hasReply = notification.hasReply,
            active = true,
            read = false,
        )
        replace(superseded + entry)
    }

    private fun setRead(ids: Set<Long>, read: Boolean) = synchronized(lock) {
        replace(_state.value.items.filter { it.id in ids && it.read != read }.map { it.copy(read = read) })
    }

    /** Writes changed or new entries to the state and the store. */
    private fun replace(changed: List<HubItem>) {
        if (changed.isEmpty()) return
        val byId = changed.associateBy { it.id }
        val kept = _state.value.items.filter { it.id !in byId }
        _state.value = _state.value.copy(items = (kept + changed).sortedByDescending { it.postTime })
        store.upsert(changed)
    }

    private fun remove(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        val gone = ids.toSet()
        _state.value = _state.value.copy(items = _state.value.items.filter { it.id !in gone })
        store.delete(gone)
    }

    /** Drops cleared entries past the retention period, then the oldest cleared ones over the cap. */
    private fun prune() {
        val cutoff = clock() - retentionMillis
        val items = _state.value.items
        val expired = items.filter { !it.active && it.postTime < cutoff }.map { it.id }.toMutableSet()
        val overflow = items.size - expired.size - maxItems
        if (overflow > 0) {
            items.asReversed()
                .filter { !it.active && it.id !in expired }
                .take(overflow)
                .mapTo(expired) { it.id }
        }
        remove(expired)
    }

    companion object {
        const val MAX_ITEMS = 5_000
        const val RETENTION_MILLIS = 90L * 24 * 60 * 60 * 1000
    }
}
