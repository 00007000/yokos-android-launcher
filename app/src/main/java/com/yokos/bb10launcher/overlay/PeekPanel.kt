package com.yokos.bb10launcher.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yokos.bb10launcher.R
import com.yokos.bb10launcher.hub.HubState
import com.yokos.bb10launcher.hub.ui.HubRow
import com.yokos.bb10launcher.ui.theme.Bb10Colors

private const val PEEK_ITEMS = 12

/** The Hub sliding in over another app while the user drags from the peek strip. */
@Composable
fun PeekPanel(progress: () -> Float, edge: PeekEdge, state: HubState) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val panelWidth = maxWidth * PeekGesture.PANEL_FRACTION
        val panelWidthPx = with(LocalDensity.current) { panelWidth.toPx() }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress() }
                .background(Color.Black.copy(alpha = 0.5f)),
        )
        Column(
            Modifier
                .align(if (edge == PeekEdge.Left) Alignment.TopStart else Alignment.TopEnd)
                .width(panelWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    val hidden = (1f - progress()) * panelWidthPx
                    translationX = if (edge == PeekEdge.Left) -hidden else hidden
                }
                .background(Bb10Colors.Black)
                .systemBarsPadding(),
        ) {
            Text(
                stringResource(R.string.hub_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp),
            )
            Text(
                stringResource(R.string.hub_unread_count, state.unreadCount),
                style = MaterialTheme.typography.bodySmall,
                color = Bb10Colors.TextDim,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            )
            HorizontalDivider(color = Bb10Colors.Divider)
            if (state.items.isEmpty()) {
                Text(
                    stringResource(R.string.hub_no_notifications),
                    color = Bb10Colors.TextDim,
                    modifier = Modifier.padding(16.dp),
                )
            }
            state.items.take(PEEK_ITEMS).forEach { item ->
                HubRow(item = item, unread = state.isUnread(item))
            }
        }
    }
}
