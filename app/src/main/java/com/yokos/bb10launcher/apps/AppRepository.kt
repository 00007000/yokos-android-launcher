package com.yokos.bb10launcher.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import com.yokos.bb10launcher.settings.LauncherSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A launchable activity, for any user profile (personal or work). */
data class AppEntry(
    val key: String,
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val user: UserHandle,
    val info: LauncherActivityInfo,
)

/** Keeps the list of launchable apps current and applies the user's saved grid order. */
class AppRepository(
    private val context: Context,
    private val settings: LauncherSettings,
    private val scope: CoroutineScope,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val installed = MutableStateFlow<List<AppEntry>>(emptyList())

    val apps: StateFlow<List<AppEntry>> =
        combine(installed, settings.appOrder) { list, order ->
            AppOrdering.order(list, order, AppEntry::key, AppEntry::label)
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = reload()
        override fun onPackageAdded(packageName: String, user: UserHandle) = reload()
        override fun onPackageChanged(packageName: String, user: UserHandle) = reload()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
    }

    init {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        reload()
    }

    fun reload() {
        scope.launch { installed.value = withContext(Dispatchers.IO) { query() } }
    }

    private fun query(): List<AppEntry> = launcherApps.profiles.flatMap { user ->
        launcherApps.getActivityList(null, user).map { info ->
            AppEntry(
                key = keyOf(info.componentName, user),
                label = info.label.toString(),
                packageName = info.componentName.packageName,
                component = info.componentName,
                user = user,
                info = info,
            )
        }
    }.filter { it.packageName != context.packageName }

    /** Packages with a launcher entry in the current profile. */
    fun launchablePackages(): Set<String> =
        installed.value.filter { it.user == Process.myUserHandle() }.mapTo(HashSet()) { it.packageName }

    fun entryFor(packageName: String): AppEntry? =
        installed.value.firstOrNull { it.packageName == packageName && it.user == Process.myUserHandle() }

    fun launch(entry: AppEntry, sourceBounds: Rect? = null) {
        runCatching { launcherApps.startMainActivity(entry.component, entry.user, sourceBounds, null) }
    }

    fun launchPackage(packageName: String): Boolean {
        val entry = entryFor(packageName) ?: return false
        launch(entry)
        return true
    }

    fun openAppInfo(entry: AppEntry) {
        runCatching { launcherApps.startAppDetailsActivity(entry.component, entry.user, null, null) }
    }

    fun uninstall(entry: AppEntry) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", entry.packageName, null))
            .putExtra(Intent.EXTRA_USER, entry.user)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            openAppInfo(entry)
        }
    }

    /** Moves an app to [toIndex] in the full grid order and saves the result. */
    suspend fun move(key: String, toIndex: Int) {
        val keys = apps.value.map { it.key }
        settings.setAppOrder(AppOrdering.move(keys, keys.indexOf(key), toIndex))
    }

    companion object {
        fun keyOf(component: ComponentName, user: UserHandle) =
            "${component.flattenToShortString()}#${user.hashCode()}"
    }
}
