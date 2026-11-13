package io.github.taehunkim.dorangori.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taehunkim.dorangori.R
import io.github.taehunkim.dorangori.data.db.RecState
import io.github.taehunkim.dorangori.data.db.RecordingEntity
import io.github.taehunkim.dorangori.ui.container
import io.github.taehunkim.dorangori.ui.formatDuration
import io.github.taehunkim.dorangori.ui.formatWhen
import kotlinx.coroutines.launch

@Composable
fun stateLabel(state: RecState): String = stringResource(
    when (state) {
        RecState.RECEIVED -> R.string.state_received
        RecState.TRANSCRIBING -> R.string.state_transcribing
        RecState.DONE -> R.string.state_done
        RecState.FAILED -> R.string.state_failed
        RecState.NO_MODEL -> R.string.state_no_model
    },
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecordingListScreen(onOpen: (Long) -> Unit, onSettings: () -> Unit) {
    val container = LocalContext.current.container
    val recs by container.dao.observeAll().collectAsStateWithLifecycle(emptyList())
    var selected by rememberSaveable { mutableStateOf<List<Long>>(emptyList()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val selecting = selected.isNotEmpty()

    Scaffold(
        topBar = {
            if (selecting) TopAppBar(
                title = { Text(stringResource(R.string.selected_count, selected.size)) },
                navigationIcon = { IconButton(onClick = { selected = emptyList() }) { Icon(Icons.Default.Close, stringResource(R.string.cancel)) } },
                actions = { IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) } },
            ) else TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = { IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, stringResource(R.string.settings_title)) } },
            )
        },
    ) { padding ->
        if (recs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Column {
                    Text(stringResource(R.string.list_empty), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.list_empty_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(recs, key = { it.id }) { rec ->
                val isSelected = rec.id in selected
                Box(
                    Modifier
                        .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                        .combinedClickable(
                            onClick = { if (selecting) selected = if (isSelected) selected - rec.id else selected + rec.id else onOpen(rec.id) },
                            onLongClick = { selected = if (isSelected) selected - rec.id else selected + rec.id },
                        ),
                ) { RecordingRow(rec) }
                HorizontalDivider()
            }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.delete_confirm_title)) },
        text = { Text(pluralStringResource(R.plurals.delete_confirm_text, selected.size, selected.size)) },
        confirmButton = {
            TextButton(onClick = {
                val ids = selected; selected = emptyList(); confirmDelete = false
                scope.launch { container.repo.delete(ids) }
            }) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun RecordingRow(rec: RecordingEntity) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        headlineContent = { Text(formatWhen(rec.recordedAt)) },
        supportingContent = {
            val preview = rec.text?.takeIf { it.isNotBlank() }
            Text(
                preview ?: if (rec.state == RecState.DONE) stringResource(R.string.notif_no_speech) else stateLabel(rec.state),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                if (rec.durationMs > 0) Text(formatDuration(rec.durationMs), style = MaterialTheme.typography.labelMedium)
                if (rec.state != RecState.DONE) Text(stateLabel(rec.state), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        },
    )
}
