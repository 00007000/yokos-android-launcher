package com.yokos.bb10launcher.frames

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.yokos.bb10launcher.launcherApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Small still pictures of apps for their Active Frames. Kept in the app's cache on this phone only. */
class FramePreviews(context: Context) {
    private val dir = File(context.cacheDir, "frame_previews").apply { mkdirs() }

    private fun file(packageName: String) = File(dir, "$packageName.jpg")

    /** Writes the picture atomically so a half-written file is never shown. */
    fun save(packageName: String, bitmap: Bitmap) {
        val tmp = File(dir, "$packageName.tmp")
        tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        tmp.renameTo(file(packageName))
    }

    fun load(packageName: String): Bitmap? {
        val f = file(packageName)
        return if (f.exists()) BitmapFactory.decodeFile(f.path) else null
    }

    fun delete(packageName: String) {
        file(packageName).delete()
    }

    fun deleteAll() {
        dir.listFiles()?.forEach { it.delete() }
    }

    private companion object {
        const val QUALITY = 75
    }
}

/** The saved preview for [packageName], reloaded whenever [refreshKey] changes. */
@Composable
fun rememberFramePreview(packageName: String, refreshKey: Any?): State<ImageBitmap?> {
    val previews = LocalContext.current.launcherApp.framePreviews
    return produceState<ImageBitmap?>(null, packageName, refreshKey) {
        value = withContext(Dispatchers.IO) { previews.load(packageName)?.asImageBitmap() }
    }
}
