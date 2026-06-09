package app.lawnchair.bb10hub

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class HubNotificationService : NotificationListenerService() {
    private val notifications = mutableListOf<NotificationItem>()
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn != null) {
            val notification = sbn.notification
            val title = notification.extras.getString(Notification.EXTRA_TITLE, "")
            val message = notification.extras.getString(Notification.EXTRA_TEXT, "")
            val item = NotificationItem(id = sbn.key, packageName = sbn.packageName, title = title ?: "", message = message ?: "", isRead = false, timestamp = System.currentTimeMillis())
            notifications.add(0, item)
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn != null) notifications.removeAll { it.id == sbn.key }
    }
    
    fun markAsRead(notificationId: String) {
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index >= 0) {
            val old = notifications[index]
            notifications[index] = old.copy(isRead = true)
        }
    }
    
    fun dismissNotification(notificationId: String) {
        cancelNotification(notificationId)
        notifications.removeAll { it.id == notificationId }
    }
    
    override fun onSnoozeNotification(notificationId: String, duration: Long) {
        super.onSnoozeNotification(notificationId, duration)
    }
    
    fun getNotificationsSorted(): List<NotificationItem> {
        return notifications.sortedByDescending { it.timestamp }
    }
}
