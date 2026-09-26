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

    private companion object {
        val APP_ORDER = stringPreferencesKey("app_order")
        val HUB_READ = stringSetPreferencesKey("hub_read")
    }
}
