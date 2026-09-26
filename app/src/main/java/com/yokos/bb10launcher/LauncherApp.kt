package com.yokos.bb10launcher

import android.app.Application
import android.content.Context
import com.yokos.bb10launcher.apps.AppRepository
import com.yokos.bb10launcher.hub.HubRepository
import com.yokos.bb10launcher.settings.LauncherSettings
import com.yokos.bb10launcher.util.IconLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Holds the process-wide repositories shared by the launcher, the Hub listener and the peek overlay. */
class LauncherApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val settings by lazy { LauncherSettings(this) }
    val icons by lazy { IconLoader(this) }
    val apps by lazy { AppRepository(this, settings, appScope) }
    val hub = HubRepository()

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            hub.restoreRead(settings.hubReadKeys.first())
            hub.state
                .map { hub.persistableReadKeys(it) }
                .distinctUntilChanged()
                .collect { settings.setHubReadKeys(it) }
        }
    }
}

val Context.launcherApp: LauncherApp
    get() = applicationContext as LauncherApp
