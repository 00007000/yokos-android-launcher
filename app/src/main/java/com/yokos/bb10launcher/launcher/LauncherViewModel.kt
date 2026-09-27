package com.yokos.bb10launcher.launcher

import android.app.Application
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yokos.bb10launcher.apps.AppEntry
import com.yokos.bb10launcher.frames.Frame
import com.yokos.bb10launcher.frames.FrameReducer
import com.yokos.bb10launcher.frames.RecentAppsSource
import com.yokos.bb10launcher.frames.UsageEvent
import com.yokos.bb10launcher.launcherApp
import com.yokos.bb10launcher.onboarding.Permissions
import com.yokos.bb10launcher.onboarding.SetupStatus
import com.yokos.bb10launcher.overlay.PeekConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val launcher = application.launcherApp
    private val settings = launcher.settings

    val apps = launcher.apps.apps
    val hub = launcher.hub
    val widgets = launcher.widgets

    private val _setup = MutableStateFlow(Permissions.status(application))
    val setup: StateFlow<SetupStatus> = _setup.asStateFlow()

    private val usageEvents = MutableStateFlow<List<UsageEvent>>(emptyList())

    val frames: StateFlow<List<Frame>> =
        combine(usageEvents, apps, settings.closedFrames) { events, entries, closed ->
            val launchable = entries.filter { it.user == Process.myUserHandle() }.mapTo(HashSet()) { it.packageName }
            FrameReducer.reduce(events, launchable, closed, exclude = setOf(application.packageName))
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val peekConfig: StateFlow<PeekConfig> =
        settings.peekConfig.stateIn(viewModelScope, SharingStarted.Eagerly, PeekConfig())

    fun setPeekConfig(config: PeekConfig) {
        viewModelScope.launch { settings.setPeekConfig(config) }
    }

    val framePreviews: StateFlow<Boolean> =
        settings.framePreviews.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setFramePreviews(enabled: Boolean) {
        viewModelScope.launch {
            settings.setFramePreviews(enabled)
            if (!enabled) withContext(Dispatchers.IO) { launcher.framePreviews.deleteAll() }
        }
    }

    val widgetFrames: StateFlow<List<Int>> =
        settings.widgetFrames.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun launch(entry: AppEntry) = launcher.apps.launch(entry)
    fun launchPackage(packageName: String) = launcher.apps.launchPackage(packageName)
    fun openAppInfo(entry: AppEntry) = launcher.apps.openAppInfo(entry)
    fun uninstall(entry: AppEntry) = launcher.apps.uninstall(entry)

    fun moveApp(key: String, toIndex: Int) {
        viewModelScope.launch { launcher.apps.move(key, toIndex) }
    }

    fun closeFrame(packageName: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val closed = settings.closedFrames.first() + (packageName to now)
            settings.setClosedFrames(FrameReducer.pruneClosed(closed, now, RecentAppsSource.WINDOW_MILLIS))
        }
    }

    fun addWidgetFrame(id: Int) {
        viewModelScope.launch { settings.setWidgetFrames(settings.widgetFrames.first() + id) }
    }

    fun removeWidgetFrame(id: Int) {
        widgets.release(id)
        viewModelScope.launch { settings.setWidgetFrames(settings.widgetFrames.first() - id) }
    }

    /** Called whenever the home screen comes back to the foreground. */
    fun onResume() {
        launcher.apps.reload()
        val status = Permissions.status(getApplication())
        _setup.value = status
        if (status.usageAccess) {
            viewModelScope.launch {
                usageEvents.value = withContext(Dispatchers.IO) { launcher.recentApps.load() }
            }
        }
    }
}
