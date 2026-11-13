package io.github.taehunkim.dorangori.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taehunkim.dorangori.R
import io.github.taehunkim.dorangori.data.db.RecState
import io.github.taehunkim.dorangori.notify.Share
import io.github.taehunkim.dorangori.ui.container
import io.github.taehunkim.dorangori.ui.formatDuration
import io.github.taehunkim.dorangori.ui.formatWhen
import io.github.taehunkim.dorangori.ui.list.stateLabel
import io.github.taehunkim.ondolsaem.sdk.OndolSaem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun RecordingDetailScreen(id: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val rec by container.dao.observe(id).collectAsStateWithLifecycle(null)
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    val current = rec

    // The recording was deleted (here, by the retention rule, or from the list): nothing left to show.
    LaunchedEffect(current == null) { if (current == null) { kotlinx.coroutines.delay(400); if (container.dao.get(id) == null) onBack() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current?.let { formatWhen(it.recordedAt) } ?: "") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                actions = { IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) } },
            )
        },
    ) { padding ->
        if (current == null) return@Scaffold
        val player = rememberPlayer(container.repo.audioFile(current))
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { runCatching { player.toggle() } }) {
                    Icon(if (player.playing) Icons.Default.Pause else Icons.Default.PlayArrow, stringResource(if (player.playing) R.string.pause else R.string.play))
                }
                val total = (player.durationMs.takeIf { it > 0 } ?: current.durationMs.toInt()).coerceAtLeast(1)
                Slider(value = player.positionMs.toFloat().coerceAtMost(total.toFloat()), onValueChange = { player.seekTo(it.toInt()) }, valueRange = 0f..total.toFloat(), modifier = Modifier.weight(1f))
                Text(formatDuration(total.toLong()), style = MaterialTheme.typography.labelMedium)
            }
            Text(stateLabel(current.state), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

            val text = current.text.orEmpty()
            when (current.state) {
                RecState.DONE ->
                    if (text.isBlank()) Text(stringResource(R.string.notif_no_speech), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else SelectionContainer { Text(text, style = MaterialTheme.typography.bodyLarge) }
                RecState.NO_MODEL -> Text(stringResource(R.string.detail_no_model), color = MaterialTheme.colorScheme.onSurfaceVariant)
                RecState.FAILED -> Text(stringResource(R.string.detail_failed, current.error.orEmpty()), color = MaterialTheme.colorScheme.error)
                else -> Unit
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (current.state == RecState.DONE && text.isNotBlank()) {
                    FilledTonalButton(onClick = {
                        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Dorangori", text))
                        Toast.makeText(context, R.string.toast_copied, Toast.LENGTH_SHORT).show()
                    }) { Text(stringResource(R.string.action_copy)) }
                    FilledTonalButton(onClick = {
                        val intent = Share.askMalbotdari(context, text)
                        if (intent == null) Toast.makeText(context, R.string.toast_malbotdari_missing, Toast.LENGTH_LONG).show() else context.startActivity(intent)
                    }) { Text(stringResource(R.string.action_ask_malbotdari)) }
                    val noteId = current.jogakboNoteId
                    FilledTonalButton(onClick = {
                        if (noteId != null) context.startActivity(io.github.taehunkim.dorangori.notify.JogakboSaver.openNote(noteId))
                        else { container.jogakboSaver.enqueue(current.id); Toast.makeText(context, R.string.toast_saving, Toast.LENGTH_SHORT).show() }
                    }) { Text(stringResource(if (noteId != null) R.string.action_open_jogakbo else R.string.action_save_jogakbo)) }
                }
                if (current.state == RecState.RECEIVED || current.state == RecState.NO_MODEL || current.state == RecState.FAILED) {
                    Button(onClick = { container.scheduler.transcribeNow(current.id) }) {
                        Text(stringResource(if (current.state == RecState.FAILED) R.string.action_retry else R.string.action_transcribe_now))
                    }
                }
                if (current.state == RecState.NO_MODEL) {
                    OutlinedButton(onClick = { launchOndolSaem(context) }) { Text(stringResource(R.string.action_open_ondolsaem)) }
                }
                if (current.state == RecState.DONE) {
                    OutlinedButton(onClick = {
                        scope.launch { container.dao.setState(current.id, RecState.RECEIVED, null); container.scheduler.transcribeNow(current.id) }
                    }) { Text(stringResource(R.string.action_retranscribe)) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.delete_confirm_title)) },
        text = { Text(androidx.compose.ui.res.pluralStringResource(R.plurals.delete_confirm_text, 1, 1)) },
        confirmButton = { TextButton(onClick = { confirmDelete = false; scope.launch { container.repo.delete(listOf(id)) } }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

internal fun launchOndolSaem(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(OndolSaem.PACKAGE)
    if (intent == null) Toast.makeText(context, R.string.toast_ondolsaem_missing, Toast.LENGTH_LONG).show() else context.startActivity(intent)
}
