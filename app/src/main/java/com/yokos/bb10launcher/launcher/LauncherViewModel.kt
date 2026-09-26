package com.yokos.bb10launcher.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yokos.bb10launcher.apps.AppEntry
import com.yokos.bb10launcher.launcherApp
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val launcher = application.launcherApp

    val apps = launcher.apps.apps

    fun launch(entry: AppEntry) = launcher.apps.launch(entry)
    fun openAppInfo(entry: AppEntry) = launcher.apps.openAppInfo(entry)
    fun uninstall(entry: AppEntry) = launcher.apps.uninstall(entry)

    fun moveApp(key: String, toIndex: Int) {
        viewModelScope.launch { launcher.apps.move(key, toIndex) }
    }

    /** Called whenever the home screen comes back to the foreground. */
    fun onResume() {
        launcher.apps.reload()
    }
}
