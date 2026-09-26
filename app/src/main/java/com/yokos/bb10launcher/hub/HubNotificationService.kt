package com.yokos.bb10launcher.hub

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.app.Person
import android.app.RemoteInput
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.yokos.bb10launcher.launcherApp
import java.util.concurrent.ConcurrentHashMap

/**
 * Feeds every user-facing notification into the [HubRepository] and carries out Hub actions
 * (open, reply, snooze, dismiss) on the live notification.
 */
class HubNotificationService : NotificationListenerService(), HubController {
    private val hub: HubRepository get() = launcherApp.hub
    private val active = ConcurrentHashMap<String, StatusBarNotification>()
    private val labels = ConcurrentHashMap<String, String>()

    override fun onListenerConnected() {
        active.clear()
        val items = activeNotifications.orEmpty().mapNotNull { sbn ->
            toItem(sbn)?.also { active[sbn.key] = sbn }
        }
        hub.controller = this
        hub.onConnected(items)
    }

    override fun onListenerDisconnected() {
        if (hub.controller === this) hub.controller = null
        hub.onDisconnected()
        active.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val item = toItem(sbn)
        if (item == null) {
            if (active.remove(sbn.key) != null) hub.remove(sbn.key)
            return
        }
        active[sbn.key] = sbn
        hub.upsert(item)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        active.remove(sbn.key)
        hub.remove(sbn.key)
    }

    override fun open(key: String): Boolean {
        val sbn = active[key] ?: return false
        val intent = sbn.notification.contentIntent ?: return false
        val sent = send(intent, fillIn = null)
        if (sent && sbn.notification.flags and Notification.FLAG_AUTO_CANCEL != 0) cancelNotification(key)
        return sent
    }

    override fun dismiss(key: String) = cancelNotification(key)

    override fun snooze(key: String, durationMillis: Long) = snoozeNotification(key, durationMillis)

    override fun reply(key: String, text: String): Boolean {
        val sbn = active[key] ?: return false
        val action = replyAction(sbn.notification) ?: return false
        val inputs = action.remoteInputs.filter { it.allowFreeFormInput }.toTypedArray()
        val results = Bundle().apply { inputs.forEach { putCharSequence(it.resultKey, text) } }
        val fillIn = Intent()
        RemoteInput.addResultsToIntent(inputs, fillIn, results)
        RemoteInput.setResultsSource(fillIn, RemoteInput.SOURCE_FREE_FORM_INPUT)
        return send(action.actionIntent, fillIn)
    }

    private fun send(pendingIntent: PendingIntent, fillIn: Intent?): Boolean {
        // Android 14 only lets the target start an activity if the sender opts in.
        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                .toBundle()
        } else {
            null
        }
        return try {
            pendingIntent.send(this, 0, fillIn, null, null, null, options)
            true
        } catch (_: PendingIntent.CanceledException) {
            false
        }
    }

    private fun toItem(sbn: StatusBarNotification): HubItem? {
        val notification = sbn.notification
        if (sbn.packageName == packageName) return null
        if (sbn.isOngoing || notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return null
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null

        val extras = notification.extras
        val title = (extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE))?.toString().orEmpty()
        val text = lastMessage(extras)
            ?: (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return null

        return HubItem(
            key = sbn.key,
            packageName = sbn.packageName,
            appLabel = labelFor(sbn.packageName),
            title = title,
            text = text,
            postTime = sbn.postTime,
            category = HubCategory.fromNotificationCategory(notification.category),
            canReply = replyAction(notification) != null,
            canOpen = notification.contentIntent != null,
        )
    }

    /** Latest line of a MessagingStyle conversation, prefixed with its sender. */
    private fun lastMessage(extras: Bundle): String? {
        val messages: Array<out Parcelable>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelableArray(Notification.EXTRA_MESSAGES, Parcelable::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        }
        val last = messages?.lastOrNull() as? Bundle ?: return null
        val text = last.getCharSequence("text")?.toString() ?: return null
        val person: Person? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            last.getParcelable("sender_person", Person::class.java)
        } else {
            @Suppress("DEPRECATION")
            last.getParcelable("sender_person")
        }
        val sender = (last.getCharSequence("sender") ?: person?.name)?.toString()
        return if (sender.isNullOrBlank()) text else "$sender: $text"
    }

    private fun replyAction(notification: Notification): Notification.Action? =
        notification.actions?.firstOrNull { action ->
            action.remoteInputs?.any { it.allowFreeFormInput } == true
        }

    private fun labelFor(pkg: String): String = labels.getOrPut(pkg) {
        try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            pkg
        }
    }
}
