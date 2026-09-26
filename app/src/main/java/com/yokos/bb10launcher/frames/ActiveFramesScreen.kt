package com.yokos.bb10launcher.frames

import android.app.Activity
import android.content.ActivityNotFoundException
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.scale
import com.yokos.bb10launcher.R
import com.yokos.bb10launcher.onboarding.SetupStatus
import com.yokos.bb10launcher.ui.theme.Bb10Colors
import com.yokos.bb10launcher.util.ageText
import com.yokos.bb10launcher.util.rememberPackageIcon
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale

/**
 * BB10 Active Frames: the clock, then cards for recently used apps and any widgets pinned as
 * live frames. Other apps' live screens can't be mirrored on Android, so an app frame shows the
 * app's icon on a tint taken from it.
 */
@Composable
fun ActiveFramesScreen(
    frames: List<Frame>,
    labels: Map<String, String>,
    widgetIds: List<Int>,
    widgets: FrameWidgetHost,
    setup: SetupStatus,
    onOpenFrame: (String) -> Unit,
    onCloseFrame: (String) -> Unit,
    onAddWidget: (Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onOpenSetup: () -> Unit,
    onGrantUsage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val addWidget = rememberAddWidgetFlow(widgets, onAddWidget)
    var removeId by remember { mutableStateOf<Int?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(2) }) { Clock(onOpenSetup) }
        if (!setup.complete) {
            item(span = { GridItemSpan(2) }) {
                Notice(stringResource(R.string.setup_banner, setup.grantedCount, SetupStatus.TOTAL), onOpenSetup)
            }
        }
        if (!setup.usageAccess) {
            item(span = { GridItemSpan(2) }) { Notice(stringResource(R.string.frames_needs_usage), onGrantUsage) }
        }
        items(widgetIds, key = { "widget:$it" }, span = { GridItemSpan(2) }) { id ->
            WidgetFrame(id, widgets, onRemove = { removeId = id })
        }
        items(frames, key = { it.packageName }) { frame ->
            AppFrame(
                frame = frame,
                label = labels[frame.packageName] ?: frame.packageName,
                onOpen = { onOpenFrame(frame.packageName) },
                onClose = { onCloseFrame(frame.packageName) },
                modifier = Modifier.animateItemPlacement(),
            )
        }
        if (setup.usageAccess && frames.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    stringResource(R.string.frames_empty),
                    color = Bb10Colors.TextDim,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
        item(span = { GridItemSpan(2) }) {
            TextButton(onClick = addWidget) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.frames_add_widget), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    removeId?.let { id ->
        AlertDialog(
            onDismissRequest = { removeId = null },
            title = { Text(stringResource(R.string.frames_remove_widget)) },
            confirmButton = {
                TextButton(onClick = { onRemoveWidget(id); removeId = null }) {
                    Text(stringResource(R.string.frames_remove_widget))
                }
            },
            dismissButton = {
                TextButton(onClick = { removeId = null }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

/** Picks a widget, runs its configuration screen if it has one, then pins it. */
@Composable
private fun rememberAddWidgetFlow(widgets: FrameWidgetHost, onAdded: (Int) -> Unit): () -> Unit {
    var pendingId by rememberSaveable { mutableIntStateOf(-1) }
    val configure = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = pendingId
        if (result.resultCode == Activity.RESULT_OK) onAdded(id) else widgets.release(id)
        pendingId = -1
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = pendingId
        if (result.resultCode != Activity.RESULT_OK) {
            widgets.release(id)
            pendingId = -1
            return@rememberLauncherForActivityResult
        }
        val configureIntent = widgets.configureIntent(id)
        if (configureIntent == null) {
            onAdded(id)
            pendingId = -1
        } else {
            try {
                configure.launch(configureIntent)
            } catch (_: RuntimeException) {
                // Not exported or missing: pin it unconfigured rather than losing it.
                onAdded(id)
                pendingId = -1
            }
        }
    }
    return {
        val id = widgets.allocate()
        pendingId = id
        try {
            pick.launch(widgets.pickIntent(id))
        } catch (_: ActivityNotFoundException) {
            widgets.release(id)
            pendingId = -1
        }
    }
}

@Composable
private fun Clock(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000 - value % 60_000)
        }
    }
    val time = remember(now) { DateFormat.getTimeFormat(context).format(Date(now)) }
    val date = remember(now) {
        val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEMMMMd")
        DateFormat.format(pattern, now).toString()
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(time, style = MaterialTheme.typography.displayMedium, color = Color.White)
            Text(date, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.8f))
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings), tint = Color.White)
        }
    }
}

@Composable
private fun Notice(text: String, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        border = BorderStroke(1.dp, Bb10Colors.Blue),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun AppFrame(
    frame: Frame,
    label: String,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon by rememberPackageIcon(frame.packageName)
    val tint = remember(icon) { icon?.averageColor() ?: Bb10Colors.SurfaceHigh }
    Box(
        modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.85f), Bb10Colors.Surface)))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
            .clickable(onClick = onOpen),
    ) {
        Text(
            ageText(System.currentTimeMillis() - frame.lastUsed),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
        )
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).size(40.dp)) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.frames_close), tint = Color.White)
        }
        icon?.let {
            Image(it, contentDescription = null, modifier = Modifier.align(Alignment.Center).size(64.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun WidgetFrame(id: Int, widgets: FrameWidgetHost, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val info = remember(id) { widgets.info(id) } ?: return
    Box(
        modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Bb10Colors.Surface),
    ) {
        AndroidView(
            factory = { context -> widgets.host.createView(context, id, info) },
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd).size(40.dp)) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.frames_remove_widget), tint = Color.White)
        }
    }
}

/** The icon's average colour, used to tint its frame. */
private fun ImageBitmap.averageColor(): Color {
    val pixel = asAndroidBitmap().scale(1, 1).getPixel(0, 0)
    return Color(pixel).copy(alpha = 1f)
}
