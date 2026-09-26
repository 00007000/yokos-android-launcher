package com.yokos.bb10launcher.hub

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Acts on the live notification behind a Hub entry. Implemented by the connected listener. */
interface HubController {
    fun open(key: String): Boolean
    fun dismiss(key: String)
    fun snooze(key: String, durationMillis: Long)
    fun reply(key: String, text: String): Boolean
}

/**
 * The single source of truth for the Hub. The notification listener feeds it, and the launcher
 * and the peek overlay observe [state] and call the actions.
 */
class HubRepository {
    private val _state = MutableStateFlow(HubState())
    val state: StateFlow<HubState> = _state.asStateFlow()

    @Volatile
    var controller: HubController? = null

    fun onConnected(items: List<HubItem>) = _state.update { state ->
        val keys = items.mapTo(HashSet()) { it.key }
        state.copy(
            connected = true,
            items = items.sortedByDescending { it.postTime },
            readKeys = state.readKeys.filterTo(HashSet()) { it in keys },
        )
    }

    fun onDisconnected() = _state.update { it.copy(connected = false) }

    /** Adds or replaces an entry. An update with a new post time counts as unread again. */
    fun upsert(item: HubItem) = _state.update { state ->
        val previous = state.items.firstOrNull { it.key == item.key }
        val items = (state.items.filter { it.key != item.key } + item).sortedByDescending { it.postTime }
        val readKeys = if (previous != null && previous.postTime != item.postTime) {
            state.readKeys - item.key
        } else {
            state.readKeys
        }
        state.copy(items = items, readKeys = readKeys)
    }

    fun remove(key: String) = _state.update { state ->
        state.copy(items = state.items.filter { it.key != key }, readKeys = state.readKeys - key)
    }

    fun markRead(keys: Collection<String>) = _state.update { it.copy(readKeys = it.readKeys + keys) }

    fun markUnread(key: String) = _state.update { it.copy(readKeys = it.readKeys - key) }

    fun markAllRead(packageName: String?) = markRead(_state.value.filtered(packageName).map { it.key })

    /** Merges read markers saved from a previous run. */
    fun restoreRead(keys: Set<String>) = _state.update { it.copy(readKeys = it.readKeys + keys) }

    /** Read markers worth saving: only keys that still exist once the listener has reported. */
    fun persistableReadKeys(state: HubState = _state.value): Set<String> =
        if (state.connected) state.readKeys.filterTo(HashSet()) { key -> state.items.any { it.key == key } }
        else state.readKeys

    fun open(key: String): Boolean {
        markRead(listOf(key))
        return controller?.open(key) ?: false
    }

    fun dismiss(key: String) {
        controller?.dismiss(key)
        remove(key)
    }

    fun snooze(key: String, durationMillis: Long) {
        controller?.snooze(key, durationMillis)
        remove(key)
    }

    fun reply(key: String, text: String): Boolean {
        val sent = controller?.reply(key, text) ?: false
        if (sent) markRead(listOf(key))
        return sent
    }
}
