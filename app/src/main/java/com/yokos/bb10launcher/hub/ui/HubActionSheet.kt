package com.yokos.bb10launcher.hub.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yokos.bb10launcher.R
import com.yokos.bb10launcher.hub.HubItem
import com.yokos.bb10launcher.hub.HubRepository
import com.yokos.bb10launcher.ui.theme.Bb10Colors

private const val MINUTE = 60_000L

/** Long-press menu for a Hub entry: open, reply inline, read state, snooze or dismiss. */
@Composable
fun HubActionSheet(
    item: HubItem,
    unread: Boolean,
    hub: HubRepository,
    onDismissRequest: () -> Unit,
) {
    var reply by remember(item.key) { mutableStateOf("") }
    fun act(block: () -> Unit) {
        block()
        onDismissRequest()
    }

    ModalBottomSheet(onDismissRequest = onDismissRequest, containerColor = Bb10Colors.Surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(
                item.title.ifBlank { item.appLabel },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Text(
                item.appLabel,
                style = MaterialTheme.typography.bodySmall,
                color = Bb10Colors.TextDim,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
            )
            if (item.canReply) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = reply,
                        onValueChange = { reply = it },
                        placeholder = { Text(stringResource(R.string.reply_hint)) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (reply.isNotBlank()) act { hub.reply(item.key, reply.trim()) }
                        }),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { act { hub.reply(item.key, reply.trim()) } },
                        enabled = reply.isNotBlank(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.action_send))
                    }
                }
            }
            HorizontalDivider(color = Bb10Colors.Divider)
            if (item.canOpen) {
                SheetAction(rememberVectorPainter(Icons.AutoMirrored.Filled.ExitToApp), stringResource(R.string.action_open)) {
                    act { hub.open(item.key) }
                }
            }
            if (unread) {
                SheetAction(rememberVectorPainter(Icons.Filled.Done), stringResource(R.string.action_mark_read)) {
                    act { hub.markRead(listOf(item.key)) }
                }
            } else {
                SheetAction(rememberVectorPainter(Icons.Filled.Email), stringResource(R.string.action_mark_unread)) {
                    act { hub.markUnread(item.key) }
                }
            }
            val snooze = painterResource(R.drawable.ic_snooze)
            SheetAction(snooze, stringResource(R.string.snooze_15m)) { act { hub.snooze(item.key, 15 * MINUTE) } }
            SheetAction(snooze, stringResource(R.string.snooze_1h)) { act { hub.snooze(item.key, 60 * MINUTE) } }
            SheetAction(snooze, stringResource(R.string.snooze_4h)) { act { hub.snooze(item.key, 240 * MINUTE) } }
            SheetAction(rememberVectorPainter(Icons.Filled.Delete), stringResource(R.string.action_dismiss)) {
                act { hub.dismiss(item.key) }
            }
        }
    }
}

@Composable
private fun SheetAction(icon: Painter, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
