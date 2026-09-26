package com.yokos.bb10launcher.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yokos.bb10launcher.apps.AppEntry
import com.yokos.bb10launcher.launcherApp
import com.yokos.bb10launcher.onboarding.Permissions
import com.yokos.bb10launcher.onboarding.SetupStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val launcher = application.launcherApp

    val apps = launcher.apps.apps
    val hub = launcher.hub

    private val _setup = MutableStateFlow(Permissions.status(application))
    val setup: StateFlow<SetupStatus> = _setup.asStateFlow()

    fun launch(entry: AppEntry) = launcher.apps.launch(entry)
    fun openAppInfo(entry: AppEntry) = launcher.apps.openAppInfo(entry)
    fun uninstall(entry: AppEntry) = launcher.apps.uninstall(entry)

    fun moveApp(key: String, toIndex: Int) {
        viewModelScope.launch { launcher.apps.move(key, toIndex) }
    }

    /** Called whenever the home screen comes back to the foreground. */
    fun onResume() {
        launcher.apps.reload()
        _setup.value = Permissions.status(getApplication())
    }
}
