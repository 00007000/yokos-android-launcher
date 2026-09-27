package com.yokos.bb10launcher.hub.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yokos.bb10launcher.R
import com.yokos.bb10launcher.hub.DayBucket
import com.yokos.bb10launcher.hub.HubAccount
import com.yokos.bb10launcher.hub.HubCategory
import com.yokos.bb10launcher.hub.HubItem
import com.yokos.bb10launcher.hub.HubRepository
import com.yokos.bb10launcher.hub.HubState
import com.yokos.bb10launcher.hub.groupByDay
import com.yokos.bb10launcher.ui.theme.Bb10Colors
import com.yokos.bb10launcher.util.ageText
import com.yokos.bb10launcher.util.rememberPackageIcon
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val RailWidth = 72.dp

/**
 * The BB10 Hub: one stream of every app's messages, with the account rail on the left.
 *
 * [hiddenFraction] is how much of the page is still off-screen (1 = hidden, 0 = fully shown).
 * While the user drags the Hub in from the Frames page, the rail is pinned to the visible edge,
 * so the unread counts "peek" in first, as they did on BB10.
 */
@Composable
fun HubScreen(
    state: HubState,
    hasAccess: Boolean,
    hub: HubRepository,
    onGrantAccess: () -> Unit,
    modifier: Modifier = Modifier,
    hiddenFraction: () -> Float = { 0f },
) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetItem by remember { mutableStateOf<HubItem?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val accounts = remember(state) { state.accounts() }
    // Fall back to "All" once the selected app has no entries left.
    val filter = selected?.takeIf { pkg -> accounts.any { it.packageName == pkg } }
    val items = remember(state, filter) { state.filtered(filter) }

    BoxWithConstraints(modifier.fillMaxSize().background(Bb10Colors.Black)) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        Column(Modifier.fillMaxSize().padding(start = RailWidth)) {
            HubHeader(
                title = accounts.firstOrNull { it.packageName == filter }?.label ?: stringResource(R.string.hub_title),
                unread = items.count(state::isUnread),
                onMarkAllRead = { hub.markAllRead(filter) },
                onClearHistory = { confirmClear = true },
            )
            when {
                // Saved history still shows if access was revoked; the prompt only replaces an empty Hub.
                !hasAccess && items.isEmpty() -> AccessNeeded(onGrantAccess)
                items.isEmpty() -> EmptyHub()
                else -> HubList(
                    items,
                    state,
                    onOpen = { hub.open(it.id) },
                    onToggleRead = { hub.toggleRead(it.id) },
                    onLongPress = { sheetItem = it },
                )
            }
        }
        AccountRail(
            accounts = accounts,
            totalUnread = state.unreadCount,
            selected = filter,
            onSelect = { selected = it },
            modifier = Modifier.graphicsLayer {
                translationX = hiddenFraction().coerceIn(0f, 1f) * widthPx
            },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.hub_clear_history)) },
            text = { Text(stringResource(R.string.hub_clear_history_desc)) },
            confirmButton = {
                TextButton(onClick = { hub.deleteAll(filter); confirmClear = false }) {
                    Text(stringResource(R.string.hub_clear_history))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }

    sheetItem?.let { item ->
        HubActionSheet(
            item = item,
            unread = state.isUnread(item),
            hub = hub,
            onDismissRequest = { sheetItem = null },
        )
    }
}

@Composable
private fun HubHeader(title: String, unread: Int, onMarkAllRead: () -> Unit, onClearHistory: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.hub_unread_count, unread),
                style = MaterialTheme.typography.bodySmall,
                color = Bb10Colors.TextDim,
            )
        }
        IconButton(onClick = onMarkAllRead, enabled = unread > 0) {
            Icon(Icons.Filled.Done, contentDescription = stringResource(R.string.hub_mark_all_read))
        }
        IconButton(onClick = onClearHistory) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.hub_clear_history))
        }
    }
}

@Composable
private fun AccountRail(
    accounts: List<HubAccount>,
    totalUnread: Int,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier.width(RailWidth).fillMaxHeight().background(Bb10Colors.Surface),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            RailEntry(selected == null, totalUnread, onClick = { onSelect(null) }) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = stringResource(R.string.hub_all),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        items(accounts, key = { it.packageName }) { account ->
            RailEntry(selected == account.packageName, account.unread, onClick = { onSelect(account.packageName) }) {
                val icon by rememberPackageIcon(account.packageName)
                icon?.let { Image(it, contentDescription = account.label, modifier = Modifier.size(32.dp)) }
            }
        }
    }
}

@Composable
private fun RailEntry(selected: Boolean, unread: Int, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(if (selected) Bb10Colors.SurfaceHigh else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(Modifier.align(Alignment.CenterStart).width(3.dp).fillMaxHeight().background(Bb10Colors.Blue))
        }
        icon()
        if (unread > 0) {
            Text(
                if (unread > 99) "99+" else unread.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .clip(CircleShape)
                    .background(Bb10Colors.Blue)
                    .padding(horizontal = 5.dp),
            )
        }
    }
}

@Composable
private fun HubList(
    items: List<HubItem>,
    state: HubState,
    onOpen: (HubItem) -> Unit,
    onToggleRead: (HubItem) -> Unit,
    onLongPress: (HubItem) -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val sections = remember(items) { groupByDay(items, System.currentTimeMillis(), zone) }
    LazyColumn(Modifier.fillMaxSize()) {
        sections.forEach { (bucket, entries) ->
            stickyHeader(key = bucket.toString()) { DayHeader(bucket) }
            items(entries, key = { it.id }) { item ->
                val current by rememberUpdatedState(item)
                // Swiping either way toggles read/unread; the row then springs back (the swipe is
                // never "confirmed"), so nothing is removed. Delete lives in the long-press menu.
                val swipeState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled) onToggleRead(current)
                        false
                    },
                )
                SwipeToDismissBox(
                    state = swipeState,
                    backgroundContent = { ReadToggleBackground(swipeState.dismissDirection, unread = !item.read) },
                    modifier = Modifier.animateItemPlacement(),
                ) {
                    HubRow(
                        item = item,
                        unread = state.isUnread(item),
                        modifier = Modifier.combinedClickable(
                            onClick = { onOpen(item) },
                            onLongClick = { onLongPress(item) },
                        ),
                    )
                }
            }
        }
    }
}

/** What a swipe will do, revealed behind the row as it slides. */
@Composable
private fun ReadToggleBackground(direction: SwipeToDismissBoxValue, unread: Boolean) {
    val label = stringResource(if (unread) R.string.action_mark_read else R.string.action_mark_unread)
    val icon = if (unread) Icons.Filled.Done else Icons.Filled.Email
    Row(
        Modifier.fillMaxSize().background(Bb10Colors.Blue).padding(horizontal = 20.dp),
        horizontalArrangement = if (direction == SwipeToDismissBoxValue.EndToStart) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@Composable
private fun DayHeader(bucket: DayBucket) {
    val label = when (bucket) {
        DayBucket.Today -> stringResource(R.string.hub_today)
        DayBucket.Yesterday -> stringResource(R.string.hub_yesterday)
        is DayBucket.On -> remember(bucket) {
            bucket.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        }
    }
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = Bb10Colors.Blue,
        modifier = Modifier.fillMaxWidth().background(Bb10Colors.Black).padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

/** One Hub entry: category stripe, app icon, sender and message, age and unread marker. */
@Composable
fun HubRow(item: HubItem, unread: Boolean, modifier: Modifier = Modifier) {
    val icon by rememberPackageIcon(item.packageName)
    Row(
        modifier.fillMaxWidth().heightIn(min = 68.dp).background(Bb10Colors.Black),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).height(68.dp).background(categoryColor(item.category)))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(36.dp)) {
            icon?.let { Image(it, contentDescription = item.appLabel, modifier = Modifier.fillMaxSize()) }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(
                item.title.ifBlank { item.appLabel },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                color = if (unread) Color.White else Bb10Colors.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.text.isNotBlank()) {
                Text(
                    item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Bb10Colors.TextDim,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(
            Modifier.padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                ageText(System.currentTimeMillis() - item.postTime),
                style = MaterialTheme.typography.labelSmall,
                color = Bb10Colors.TextDim,
            )
            if (unread) Box(Modifier.size(8.dp).clip(CircleShape).background(Bb10Colors.Blue))
        }
    }
}

@Composable
fun categoryColor(category: HubCategory): Color = colorResource(
    when (category) {
        HubCategory.Message -> R.color.hub_notification_message
        HubCategory.Social -> R.color.hub_notification_social
        HubCategory.Call -> R.color.hub_notification_call
        HubCategory.Alarm -> R.color.hub_notification_alarm
        HubCategory.Error -> R.color.hub_notification_error
        HubCategory.Progress -> R.color.hub_notification_progress
        HubCategory.Default -> R.color.hub_notification_default
    },
)

@Composable
private fun EmptyHub() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.hub_no_notifications), color = Bb10Colors.TextDim)
    }
}

@Composable
private fun AccessNeeded(onGrantAccess: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.hub_needs_access),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onGrantAccess) { Text(stringResource(R.string.setup_grant)) }
    }
}
