package com.yokos.bb10launcher.onboarding

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yokos.bb10launcher.R
import com.yokos.bb10launcher.launcher.startSafely
import com.yokos.bb10launcher.ui.theme.Bb10Colors

/** Checklist of the special accesses the launcher needs, each with a button to grant it. */
@Composable
fun SetupScreen(
    status: SetupStatus,
    onClose: () -> Unit,
    extraContent: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    fun open(intent: Intent) = context.startSafely(intent)
    // The role dialog only works when started for a result; it checks who is asking.
    val requestHome = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    Column(
        Modifier
            .fillMaxSize()
            .background(Bb10Colors.Black)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
            }
        }
        SetupItem(
            title = stringResource(R.string.setup_home),
            description = stringResource(R.string.setup_home_desc),
            granted = status.isDefaultHome,
            onGrant = {
                try {
                    requestHome.launch(Permissions.homeRoleIntent(context))
                } catch (_: ActivityNotFoundException) {
                    open(Intent(Settings.ACTION_HOME_SETTINGS))
                }
            },
        )
        SetupItem(
            title = stringResource(R.string.setup_notifications),
            description = stringResource(R.string.setup_notifications_desc),
            granted = status.notificationAccess,
            onGrant = { open(Permissions.notificationAccessIntent(context)) },
        )
        SetupItem(
            title = stringResource(R.string.setup_usage),
            description = stringResource(R.string.setup_usage_desc),
            granted = status.usageAccess,
            onGrant = { open(Permissions.usageAccessIntent(context)) },
        )
        extraContent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            HorizontalDivider(color = Bb10Colors.Divider, modifier = Modifier.padding(top = 8.dp))
            Text(
                stringResource(R.string.setup_restricted),
                style = MaterialTheme.typography.bodyMedium,
                color = Bb10Colors.TextDim,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            TextButton(onClick = { open(Permissions.appInfoIntent(context)) }, modifier = Modifier.padding(start = 8.dp)) {
                Text(stringResource(R.string.setup_open_app_info))
            }
        }
    }
}

@Composable
fun SetupItem(title: String, description: String, granted: Boolean, onGrant: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = Bb10Colors.TextDim)
        }
        if (granted) {
            Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.setup_granted), tint = Bb10Colors.Blue)
        } else {
            Button(onClick = onGrant) { Text(stringResource(R.string.setup_grant)) }
        }
    }
}
