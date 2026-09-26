package com.yokos.bb10launcher.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "launcher")

/** Persistent launcher preferences backed by DataStore. */
class LauncherSettings(context: Context) {
    private val store = context.applicationContext.dataStore

    /** App grid order as component keys; apps missing from it are appended alphabetically. */
    val appOrder: Flow<List<String>> = store.data.map { prefs ->
        prefs[APP_ORDER]?.split('\n')?.filter { it.isNotEmpty() }.orEmpty()
    }

    suspend fun setAppOrder(keys: List<String>) {
        store.edit { it[APP_ORDER] = keys.joinToString("\n") }
    }

    /** Notification keys the user has already seen in the Hub. */
    val hubReadKeys: Flow<Set<String>> = store.data.map { it[HUB_READ].orEmpty() }

    suspend fun setHubReadKeys(keys: Set<String>) {
        store.edit { it[HUB_READ] = keys }
    }

    /** When the user last closed each Active Frame, by package. */
    val closedFrames: Flow<Map<String, Long>> = store.data.map { prefs ->
        prefs[CLOSED_FRAMES].orEmpty().mapNotNull { entry ->
            val pkg = entry.substringBeforeLast('=')
            entry.substringAfterLast('=').toLongOrNull()?.let { pkg to it }
        }.toMap()
    }

    suspend fun setClosedFrames(closed: Map<String, Long>) {
        store.edit { prefs -> prefs[CLOSED_FRAMES] = closed.mapTo(HashSet()) { (pkg, time) -> "$pkg=$time" } }
    }

    /** App widget ids pinned as live frames, in display order. */
    val widgetFrames: Flow<List<Int>> = store.data.map { prefs ->
        prefs[WIDGET_FRAMES]?.split(',')?.mapNotNull { it.toIntOrNull() }.orEmpty()
    }

    suspend fun setWidgetFrames(ids: List<Int>) {
        store.edit { it[WIDGET_FRAMES] = ids.joinToString(",") }
    }

    private companion object {
        val APP_ORDER = stringPreferencesKey("app_order")
        val HUB_READ = stringSetPreferencesKey("hub_read")
        val CLOSED_FRAMES = stringSetPreferencesKey("closed_frames")
        val WIDGET_FRAMES = stringPreferencesKey("widget_frames")
    }
}
