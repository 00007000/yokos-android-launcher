package app.lawnchair.bb10hub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class Bb10HubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF1A1A1A)) {
                BB10HubScreen(notifications = notifications, onDismissNotification = { }, onMarkAsRead = { }, onSnoozeNotification = { _, _ -> }, onClose = { finish() })
            }
        }
    }
}
