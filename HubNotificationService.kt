package app.lawnchair.bb10hub

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import android.content.Intent
import android.util.Log

/**
 * NotificationListenerService that captures system notifications
 * and makes them available to the BB10 Hub overlay
 */
class HubNotificationService : NotificationListenerService() {

    companion object {
        const val TAG = "HubNotificationService"
        const val ACTION_NOTIFICATION_UPDATE = "app.lawnchair.bb10hub.NOTIFICATION_UPDATE"
        const val EXTRA_NOTIFICATIONS = "notifications"
        const val PREF_NAME = "hub_notifications"
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val notificationMap = mutableMapOf<String, NotificationData>()

    data class NotificationData(
        val key: String,
        val packageName: String,
        val title: String,
        val text: String,
        val timestamp: Long,
        val isRead: Boolean = false,
        val isSnoozed: Boolean = false,
        val category: String = "",
        val smallIcon: Int = 0
    )

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification Listener Connected")
        loadActiveNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        Log.d(TAG, "Notification Posted: ${sbn.packageName}")
        
        val notification = parseNotification(sbn)
        notificationMap[sbn.key] = notification
        broadcastUpdate()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        Log.d(TAG, "Notification Removed: ${sbn.packageName}")
        
        notificationMap.remove(sbn.key)
        broadcastUpdate()
    }

    private fun loadActiveNotifications() {
        scope.launch {
            val notifications = activeNotifications
            if (notifications != null) {
                notificationMap.clear()
                for (sbn in notifications) {
                    val notification = parseNotification(sbn)
                    notificationMap[sbn.key] = notification
                }
                broadcastUpdate()
            }
        }
    }

    private fun parseNotification(sbn: StatusBarNotification): NotificationData {
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString(NotificationCompat.EXTRA_TITLE) ?: "Notification"
        val text = extras.getString(NotificationCompat.EXTRA_TEXT) ?: ""
        val category = notification.category ?: ""
        val smallIcon = notification.smallIcon

        return NotificationData(
            key = sbn.key,
            packageName = sbn.packageName,
            title = title,
            text = text,
            timestamp = sbn.postTime,
            category = category,
            smallIcon = smallIcon
        )
    }

    fun markAsRead(key: String) {
        notificationMap[key]?.let {
            notificationMap[key] = it.copy(isRead = true)
            broadcastUpdate()
        }
    }

    fun snoozeNotification(key: String, durationMs: Long = 60000) {
        notificationMap[key]?.let {
            notificationMap[key] = it.copy(isSnoozed = true)
            // Auto-unsnooze after duration
            scope.launch {
                delay(durationMs)
                notificationMap[key] = it.copy(isSnoozed = false)
                broadcastUpdate()
            }
            broadcastUpdate()
        }
    }

    fun dismissNotification(key: String) {
        try {
            cancelNotification(key)
            notificationMap.remove(key)
            broadcastUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing notification", e)
        }
    }

    fun getNotificationsSorted(): List<NotificationData> {
        return notificationMap.values
            .filter { !it.isSnoozed }
            .sortedByDescending { it.timestamp }
    }

    private fun broadcastUpdate() {
        val intent = Intent(ACTION_NOTIFICATION_UPDATE).apply {
            `package` = packageName
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
