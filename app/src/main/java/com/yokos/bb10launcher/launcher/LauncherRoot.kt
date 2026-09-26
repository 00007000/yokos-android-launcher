package com.yokos.bb10launcher.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yokos.bb10launcher.R
import com.yokos.bb10launcher.apps.AppActionsDialog
import com.yokos.bb10launcher.apps.AppEntry
import com.yokos.bb10launcher.apps.AppGridPage
import com.yokos.bb10launcher.apps.AppOrdering
import com.yokos.bb10launcher.ui.theme.Bb10Colors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

const val HUB_PAGE = 0
const val FRAMES_PAGE = 1
const val FIRST_APP_PAGE = 2

/** The BB10 home: Hub, Active Frames and the app grid pages side by side in one pager. */
@Composable
fun LauncherRoot(commands: Flow<HomeCommand>, viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val appPages = remember(apps) { AppOrdering.pages(apps) }
    val pagerState = rememberPagerState(initialPage = FRAMES_PAGE) { FIRST_APP_PAGE + appPages.size }
    val scope = rememberCoroutineScope()

    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }

    LaunchedEffect(commands) {
        commands.collect { command ->
            searchOpen = false
            editMode = false
            menuApp = null
            val target = if (command == HomeCommand.OpenHub) HUB_PAGE else FRAMES_PAGE
            pagerState.animateScrollToPage(target)
        }
    }

    BackHandler(enabled = searchOpen || editMode || pagerState.currentPage != FRAMES_PAGE) {
        when {
            searchOpen -> searchOpen = false
            editMode -> editMode = false
            else -> scope.launch { pagerState.animateScrollToPage(FRAMES_PAGE) }
        }
    }

    Box(Modifier.fillMaxSize().background(Bb10Colors.Scrim)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            if (editMode) {
                EditModeBar(onDone = { editMode = false })
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                beyondBoundsPageCount = 1,
                key = { it },
            ) { page ->
                when (page) {
                    HUB_PAGE -> HubPagePlaceholder()
                    FRAMES_PAGE -> FramesPagePlaceholder()
                    else -> {
                        val pageIndex = page - FIRST_APP_PAGE
                        AppGridPage(
                            apps = appPages.getOrElse(pageIndex) { emptyList() },
                            pageStart = pageIndex * AppOrdering.PAGE_SIZE,
                            editMode = editMode,
                            onEnterEditMode = { editMode = true },
                            onLaunch = viewModel::launch,
                            onMove = viewModel::moveApp,
                            onShowMenu = { menuApp = it },
                        )
                    }
                }
            }
            PageIndicator(pagerState, appPageCount = appPages.size) { page ->
                scope.launch { pagerState.animateScrollToPage(page) }
            }
            ActionBar(
                onPhone = { context.startSafely(Intent(Intent.ACTION_DIAL)) },
                onSearch = { searchOpen = true },
                onCamera = { context.startSafely(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) },
            )
        }

        AnimatedVisibility(searchOpen, enter = fadeIn(), exit = fadeOut()) {
            SearchOverlay(
                apps = apps,
                onLaunch = { searchOpen = false; viewModel.launch(it) },
                onClose = { searchOpen = false },
            )
        }
    }

    menuApp?.let { app ->
        AppActionsDialog(
            app = app,
            onDismiss = { menuApp = null },
            onAppInfo = { viewModel.openAppInfo(app) },
            onUninstall = { viewModel.uninstall(app) },
        )
    }
}

@Composable
private fun HubPagePlaceholder() {
    Box(Modifier.fillMaxSize().background(Bb10Colors.Black), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.hub_title), style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun FramesPagePlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.frames_empty), color = Bb10Colors.TextDim)
    }
}

@Composable
private fun EditModeBar(onDone: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Bb10Colors.Surface).padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.apps_edit_hint),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDone) { Text(stringResource(R.string.apps_done)) }
    }
}

/** BB10-style page markers: a Hub glyph, a Frames glyph, then one dot per app page. */
@Composable
private fun PageIndicator(pagerState: PagerState, appPageCount: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(28.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val current = pagerState.currentPage
        fun tint(page: Int) = if (page == current) Bb10Colors.Blue else Color.White.copy(alpha = 0.55f)
        Icon(
            Icons.Filled.Notifications,
            contentDescription = stringResource(R.string.hub_title),
            tint = tint(HUB_PAGE),
            modifier = Modifier.size(16.dp).clickable { onSelect(HUB_PAGE) },
        )
        Icon(
            painterResource(R.drawable.ic_frames),
            contentDescription = stringResource(R.string.frames_title),
            tint = tint(FRAMES_PAGE),
            modifier = Modifier.padding(horizontal = 10.dp).size(14.dp).clickable { onSelect(FRAMES_PAGE) },
        )
        repeat(appPageCount) { index ->
            val page = FIRST_APP_PAGE + index
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (page == current) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(tint(page))
                    .clickable { onSelect(page) },
            )
        }
    }
}

/** The bottom bar BB10 kept on every home page: Phone, Search, Camera. */
@Composable
private fun ActionBar(onPhone: () -> Unit, onSearch: () -> Unit, onCamera: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(Color.Black.copy(alpha = 0.6f)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionBarButton(Icons.Filled.Call, stringResource(R.string.action_phone), onPhone)
        ActionBarButton(Icons.Filled.Search, stringResource(R.string.action_search), onSearch)
        IconButton(onClick = onCamera) {
            Icon(painterResource(R.drawable.ic_camera), stringResource(R.string.action_camera), tint = Color.White)
        }
    }
}

@Composable
private fun ActionBarButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = label, tint = Color.White)
    }
}

fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // Nothing on this device handles it.
    } catch (_: SecurityException) {
        // The handler isn't exported to us.
    }
}
