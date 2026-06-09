package app.lawnchair.bb10hub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NotificationItem(val id: String, val packageName: String, val title: String, val message: String, val isRead: Boolean = false, val timestamp: Long = System.currentTimeMillis())

@Composable
fun BB10HubScreen(notifications: List<NotificationItem>, onDismissNotification: (String) -> Unit, onMarkAsRead: (String) -> Unit, onSnoozeNotification: (String, Long) -> Unit, onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
        Column(modifier = Modifier.fillMaxSize()) {
            HubHeader(onClose)
            if (notifications.isEmpty()) EmptyHubContent() else LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(notifications) { notification -> NotificationCard(notification = notification, onDismiss = { onDismissNotification(notification.id) }, onMarkAsRead = { onMarkAsRead(notification.id) }) }
            }
        }
    }
}

@Composable
fun HubHeader(onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF2A2A2A)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Notifications", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        IconButton(onClick = onClose) { Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
    }
}

@Composable
fun NotificationCard(notification: NotificationItem, onDismiss: () -> Unit, onMarkAsRead: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(notification.packageName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text(notification.title, color = Color(0xFFBBBBBB), fontSize = 12.sp, maxLines = 2)
                    Text(notification.message, color = Color(0xFF999999), fontSize = 11.sp, maxLines = 3)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

@Composable
fun EmptyHubContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Default.Notifications, contentDescription = "No notifications", tint = Color(0xFF666666), modifier = Modifier.size(64.dp))
            Text("No notifications", color = Color(0xFF999999), fontSize = 16.sp)
        }
    }
}
