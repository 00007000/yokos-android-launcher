package com.yokos.bb10launcher.launcher

import android.app.SearchManager
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.yokos.bb10launcher.R
import com.yokos.bb10launcher.apps.AppEntry
import com.yokos.bb10launcher.apps.AppOrdering
import com.yokos.bb10launcher.ui.theme.Bb10Colors
import com.yokos.bb10launcher.util.rememberAppIcon
import androidx.compose.foundation.Image

/** BB10 universal search: apps first, then a web search fallback. */
@Composable
fun SearchOverlay(
    apps: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    onClose: () -> Unit,
    extraResults: @Composable (query: String) -> Unit = {},
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    val appMatches = remember(apps, query) { AppOrdering.search(apps, query, AppEntry::label) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(Modifier.fillMaxSize().background(Bb10Colors.Black.copy(alpha = 0.94f)).systemBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.apps_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { appMatches.firstOrNull()?.let(onLaunch) }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Bb10Colors.SurfaceHigh,
                    unfocusedContainerColor = Bb10Colors.SurfaceHigh,
                    focusedIndicatorColor = Bb10Colors.Blue,
                ),
                modifier = Modifier.weight(1f).focusRequester(focus),
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close), tint = Color.White)
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            if (appMatches.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.search_apps)) }
                items(appMatches, key = { it.key }) { app ->
                    AppResultRow(app, onClick = { onLaunch(app) })
                }
            }
            item { extraResults(query) }
            if (query.isNotBlank()) {
                item {
                    Text(
                        stringResource(R.string.search_web, query.trim()),
                        color = Bb10Colors.Blue,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onClose()
                                context.startSafely(
                                    Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query.trim()),
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Bb10Colors.Blue,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun AppResultRow(app: AppEntry, onClick: () -> Unit) {
    val icon by rememberAppIcon(app)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp)) {
            icon?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
        }
        Spacer(Modifier.width(14.dp))
        Text(app.label, style = MaterialTheme.typography.bodyLarge)
    }
}
