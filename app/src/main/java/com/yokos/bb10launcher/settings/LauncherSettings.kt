package com.yokos.bb10launcher.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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

    private companion object {
        val APP_ORDER = stringPreferencesKey("app_order")
    }
}
