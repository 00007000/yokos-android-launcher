package com.yokos.bb10launcher.frames

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent

/** Hosts the widgets the user pinned as live Active Frames. */
class FrameWidgetHost(context: Context) {
    val host = AppWidgetHost(context.applicationContext, HOST_ID)
    val manager: AppWidgetManager = AppWidgetManager.getInstance(context.applicationContext)

    fun allocate(): Int = host.allocateAppWidgetId()

    fun release(id: Int) = host.deleteAppWidgetId(id)

    fun info(id: Int): AppWidgetProviderInfo? = manager.getAppWidgetInfo(id)

    /** The system widget picker; it also asks the user to allow binding. */
    fun pickIntent(id: Int): Intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)

    /** The widget's own setup screen, if it has one. */
    fun configureIntent(id: Int): Intent? {
        val configure = info(id)?.configure ?: return null
        return Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
            .setComponent(configure)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
    }

    private companion object {
        const val HOST_ID = 1010
    }
}
