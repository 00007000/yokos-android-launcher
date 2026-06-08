package app.lawnchair.bb10hub

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.launch

/**
 * Compose UI for BB10 Hub displaying notifications with tab filtering
 * Features: Tab navigation (Reitern), colored category stripes, per-notification actions, springy animations
 */
@Composable
fun BB10HubScreen(
    onDismiss: () -> Unit,
    onMarkRead: (String) -> Unit,
    onSnooze: (String) -> Unit,
    onDismissNotification: (String) -> Unit
) {
    var notifications by remember { mutableStateOf<List<HubNotificationService.NotificationData>>(emptyList()) }
    var selectedTab by remember { mutableStateOf("all") }

    LaunchedEffect(Unit) {
        // In a real implementation, observe the notification service here
    }

    // Get unique app packages
    val appTabs = remember(notifications) {
        listOf("all") + notifications.map { it.packageName }.distinct().sorted()
    }

    // Filter notifications based on selected tab
    val filteredNotifications = remember(notifications, selectedTab) {
        if (selectedTab == "all") {
            notifications
        } else {
            notifications.filter { it.packageName == selectedTab }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        // Hub Overlay Panel
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.85f)
                .align(Alignment.CenterStart)
                .background(MaterialTheme.colorScheme.surface)
                .padding(0.dp)
                .clickable(enabled = false) {} // Prevent clicks from propagating
        ) {
            // Header
            HubHeader(
                notificationCount = notifications.size,
                onClose = onDismiss
            )

            // Tab Bar (Reitern)
            TabBar(
                tabs = appTabs,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            // Notifications List
            if (filteredNotifications.isEmpty()) {
                EmptyHubContent()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        filteredNotifications,
                        key = { it.key }
                    ) { notification ->
                        NotificationCard(
                            notification = notification,
                            onMarkRead = { onMarkRead(notification.key) },
                            onSnooze = { onSnooze(notification.key) },
                            onDismiss = { onDismissNotification(notification.key) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabBar(
    tabs: List<String>,
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                TabItem(
                    label = if (tab == "all") "All" else getAppNameFromPackage(tab),
                    isSelected = tab == selectedTab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(36.dp)
            .clickable(onClick = onClick),
        color = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun getAppNameFromPackage(packageName: String): String {
    return when (packageName) {
        "com.instagram.android" -> "Instagram"
        "com.whatsapp" -> "WhatsApp"
        "com.google.android.gm" -> "Gmail"
        "com.android.messaging" -> "Messages"
        "com.google.android.dialer" -> "Phone"
        "com.facebook.katana" -> "Facebook"
        "com.twitter.android" -> "Twitter"
        "com.spotify.music" -> "Spotify"
        "com.netflix.mediaclient" -> "Netflix"
        else -> packageName.substringAfterLast(".").take(12)
    }
}

@Composable
private fun HubHeader(
    notificationCount: Int,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Hub",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$notificationCount notifications",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Hub",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: HubNotificationService.NotificationData,
    onMarkRead: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = true,
        enter = slideInHorizontally(
            initialOffsetX = { -it }
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { -it }
        ) + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category Color Stripe
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            getCategoryColor(notification.category)
                        )
                )

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                        .clickable { isExpanded = !isExpanded }
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = notification.text,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text(
                        text = formatTime(notification.timestamp),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    // Action Buttons
                    if (isExpanded) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionButton(
                                icon = Icons.Default.Done,
                                label = "Read",
                                onClick = {
                                    onMarkRead()
                                    isExpanded = false
                                },
                                modifier = Modifier.weight(1f)
                            )

                            ActionButton(
                                icon = Icons.Default.Schedule,
                                label = "Snooze",
                                onClick = {
                                    onSnooze()
                                    isExpanded = false
                                },
                                modifier = Modifier.weight(1f)
                            )

                            ActionButton(
                                icon = Icons.Default.Close,
                                label = "Dismiss",
                                onClick = {
                                    onDismiss()
                                    isExpanded = false
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.material.icons.Icons.Filled,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun EmptyHubContent() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = "No Notifications",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No notifications",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getCategoryColor(category: String): Color {
    return when (category) {
        "social" -> Color(0xFF4CAF50) // Green
        "message", "email" -> Color(0xFF2196F3) // Blue
        "alarm" -> Color(0xFFFFC107) // Amber
        "call" -> Color(0xFF9C27B0) // Purple
        "error" -> Color(0xFFF44336) // Red
        "progress" -> Color(0xFF00BCD4) // Cyan
        else -> Color(0xFF9E9E9E) // Gray
    }
}

private fun formatTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60000 -> "now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}
