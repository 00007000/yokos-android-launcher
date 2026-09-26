package com.yokos.bb10launcher.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.yokos.bb10launcher.apps.AppEntry
import com.yokos.bb10launcher.launcherApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Loads app icons off the main thread and keeps recently used ones in memory. */
class IconLoader(context: Context) {
    private val packageManager = context.packageManager
    private val sizePx = (context.resources.displayMetrics.density * ICON_DP).roundToInt()
    private val cache = LruCache<String, ImageBitmap>(CACHE_SIZE)

    fun cached(key: String): ImageBitmap? = cache.get(key)

    suspend fun appIcon(entry: AppEntry): ImageBitmap? =
        load(entry.key) { entry.info.getBadgedIcon(0) }

    suspend fun packageIcon(packageName: String): ImageBitmap? =
        load(packageKey(packageName)) { packageManager.getApplicationIcon(packageName) }

    private suspend fun load(key: String, drawable: () -> Drawable): ImageBitmap? {
        cache.get(key)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) {
            runCatching { drawable().toBitmap(sizePx, sizePx).asImageBitmap() }.getOrNull()
        } ?: return null
        cache.put(key, bitmap)
        return bitmap
    }

    companion object {
        private const val ICON_DP = 56
        private const val CACHE_SIZE = 256

        fun packageKey(packageName: String) = "pkg:$packageName"
    }
}

@Composable
fun rememberAppIcon(entry: AppEntry): State<ImageBitmap?> {
    val icons = LocalContext.current.launcherApp.icons
    return produceState(icons.cached(entry.key), entry.key) { value = icons.appIcon(entry) }
}

@Composable
fun rememberPackageIcon(packageName: String): State<ImageBitmap?> {
    val icons = LocalContext.current.launcherApp.icons
    return produceState(icons.cached(IconLoader.packageKey(packageName)), packageName) {
        value = icons.packageIcon(packageName)
    }
}
