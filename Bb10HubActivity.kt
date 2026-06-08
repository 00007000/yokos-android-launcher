package app.lawnchair.bb10hub

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import app.lawnchair.bb10hub.HubNotificationService.Companion.ACTION_NOTIFICATION_UPDATE

/**
 * Activity that displays the BB10 Hub notification overlay
 * Triggered by left swipe gesture from launcher
 */
class Bb10HubActivity : ComponentActivity() {

    companion object {
        const val TAG = "Bb10HubActivity"
    }

    private lateinit var notificationUpdateReceiver: BroadcastReceiver
    private var notificationService: HubNotificationService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "BB10 Hub Activity Created")

        setupNotificationReceiver()

        setContent {
            MaterialTheme {
                BB10HubScreen(
                    onDismiss = { finish() },
                    onMarkRead = { key -> markNotificationAsRead(key) },
                    onSnooze = { key -> snoozeNotification(key) },
                    onDismiss = { key -> dismissNotification(key) }
                )
            }
        }
    }

    private fun setupNotificationReceiver() {
        notificationUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(TAG, "Notification update broadcast received")
                // Trigger recomposition in Compose
            }
        }

        val intentFilter = IntentFilter(ACTION_NOTIFICATION_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(
                this,
                notificationUpdateReceiver,
                intentFilter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } else {
            registerReceiver(notificationUpdateReceiver, intentFilter)
        }
    }

    private fun markNotificationAsRead(key: String) {
        getNotificationService()?.markAsRead(key)
    }

    private fun snoozeNotification(key: String) {
        getNotificationService()?.snoozeNotification(key, 60000) // 1 minute
    }

    private fun dismissNotification(key: String) {
        getNotificationService()?.dismissNotification(key)
    }

    private fun getNotificationService(): HubNotificationService? {
        return try {
            // In a real implementation, you would bind to the service
            // For now, we access it via reflection or another mechanism
            val listenerServices = currentFocus?.context?.let { context ->
                (context.getSystemService(NOTIFICATION_SERVICE) as? android.app.NotificationManager)
            }
            notificationService
        } catch (e: Exception) {
            Log.e(TAG, "Error getting notification service", e)
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(notificationUpdateReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }
}
