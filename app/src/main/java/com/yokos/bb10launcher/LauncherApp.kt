package com.yokos.bb10launcher

import android.app.Application
import android.content.Context
import com.yokos.bb10launcher.apps.AppRepository
import com.yokos.bb10launcher.frames.FramePreviews
import com.yokos.bb10launcher.frames.FrameWidgetHost
import com.yokos.bb10launcher.frames.RecentAppsSource
import com.yokos.bb10launcher.hub.HubRepository
import com.yokos.bb10launcher.hub.SqliteHubStore
import com.yokos.bb10launcher.settings.LauncherSettings
import com.yokos.bb10launcher.util.IconLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Holds the process-wide repositories shared by the launcher, the Hub listener and the peek overlay. */
class LauncherApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val settings by lazy { LauncherSettings(this) }
    val icons by lazy { IconLoader(this) }
    val apps by lazy { AppRepository(this, settings, appScope) }
    val hub by lazy { HubRepository(SqliteHubStore(this), openApp = { apps.launchPackage(it) }) }
    val recentApps by lazy { RecentAppsSource(this) }
    val widgets by lazy { FrameWidgetHost(this) }
    val framePreviews by lazy { FramePreviews(this) }
}

val Context.launcherApp: LauncherApp
    get() = applicationContext as LauncherApp
