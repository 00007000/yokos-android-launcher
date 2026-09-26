package com.yokos.bb10launcher.apps

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yokos.bb10launcher.R
import com.yokos.bb10launcher.apps.AppOrdering.COLUMNS
import com.yokos.bb10launcher.apps.AppOrdering.ROWS
import com.yokos.bb10launcher.util.rememberAppIcon

/**
 * One page of the BB10 app grid. Long-press starts rearranging: drag an icon onto another cell to
 * move it there. In rearrange mode a tap opens the app's menu instead of launching it.
 */
@Composable
fun AppGridPage(
    apps: List<AppEntry>,
    pageStart: Int,
    editMode: Boolean,
    onEnterEditMode: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onMove: (key: String, toIndex: Int) -> Unit,
    onShowMenu: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val wiggle = rememberWiggle(editMode)

    BoxWithConstraints(modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp)) {
        val cellWidth = maxWidth / COLUMNS
        val cellHeight = maxHeight / ROWS
        val density = LocalDensity.current
        val cellWidthPx = with(density) { cellWidth.toPx() }
        val cellHeightPx = with(density) { cellHeight.toPx() }

        apps.forEachIndexed { index, app ->
            val column = index % COLUMNS
            val row = index / COLUMNS
            val dragging = app.key == dragKey
            Box(
                Modifier
                    .offset(x = cellWidth * column, y = cellHeight * row)
                    .size(cellWidth, cellHeight)
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer {
                        if (dragging) {
                            translationX = dragOffset.x
                            translationY = dragOffset.y
                            scaleX = 1.15f
                            scaleY = 1.15f
                        } else if (editMode) {
                            rotationZ = if ((index % 2) == 0) wiggle() else -wiggle()
                        }
                    }
                    .pointerInput(app.key, index, pageStart, apps.size, cellWidthPx, cellHeightPx) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                dragKey = app.key
                                dragOffset = Offset.Zero
                                onEnterEditMode()
                            },
                            onDragEnd = {
                                val targetColumn = ((column + 0.5f) * cellWidthPx + dragOffset.x)
                                    .div(cellWidthPx).toInt().coerceIn(0, COLUMNS - 1)
                                val targetRow = ((row + 0.5f) * cellHeightPx + dragOffset.y)
                                    .div(cellHeightPx).toInt().coerceIn(0, ROWS - 1)
                                val target = (targetRow * COLUMNS + targetColumn).coerceAtMost(apps.lastIndex)
                                if (target != index) onMove(app.key, pageStart + target)
                                dragKey = null
                            },
                            onDragCancel = { dragKey = null },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount
                            },
                        )
                    }
                    .clickable { if (editMode) onShowMenu(app) else onLaunch(app) },
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(app)
            }
        }
    }
}

@Composable
private fun rememberWiggle(enabled: Boolean): () -> Float {
    if (!enabled) return { 0f }
    val transition = rememberInfiniteTransition(label = "wiggle")
    val angle by transition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(140, easing = LinearEasing), RepeatMode.Reverse),
        label = "wiggleAngle",
    )
    return { angle }
}

@Composable
fun AppIcon(app: AppEntry, modifier: Modifier = Modifier) {
    val icon by rememberAppIcon(app)
    Column(
        modifier.fillMaxWidth().padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(12.dp))) {
            icon?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            app.label,
            style = MaterialTheme.typography.labelMedium.copy(
                shadow = Shadow(Color.Black, Offset(0f, 1f), blurRadius = 4f),
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** App menu shown when an icon is tapped while rearranging. */
@Composable
fun AppActionsDialog(
    app: AppEntry,
    onDismiss: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label) },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onUninstall() }) { Text(stringResource(R.string.app_uninstall)) }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss(); onAppInfo() }) { Text(stringResource(R.string.app_info)) }
        },
    )
}
