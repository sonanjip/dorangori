package io.github.taehunkim.dorangori.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.wearable.Wearable
import io.github.taehunkim.dorangori.BuildConfig
import io.github.taehunkim.dorangori.R
import io.github.taehunkim.dorangori.data.prefs.AsrMode
import io.github.taehunkim.dorangori.data.prefs.SettingsStore
import io.github.taehunkim.dorangori.ml.ModelHost
import io.github.taehunkim.dorangori.ui.container
import io.github.taehunkim.dorangori.ui.detail.launchOndolSaem
import io.github.taehunkim.dorangori.ui.formatMinutesOfDay
import io.github.taehunkim.ondolsaem.sdk.IdlePreset
import io.github.taehunkim.ondolsaem.sdk.OndolSaem
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val settings = container.settings
    val scope = rememberCoroutineScope()
    val mode by settings.asrMode.collectAsStateWithLifecycle(AsrMode.IMMEDIATE)
    val minutes by settings.scheduleMinutes.collectAsStateWithLifecycle(SettingsStore.DEFAULT_SCHEDULE_MINUTES)
    val chargingOnly by settings.scheduleChargingOnly.collectAsStateWithLifecycle(true)
    val preset by settings.idlePreset.collectAsStateWithLifecycle(IdlePreset.DEFAULT)
    val retention by settings.retentionDays.collectAsStateWithLifecycle(0)
    var pickTime by remember { mutableStateOf(false) }

    // Re-read the live status (notifications, watch, OndolSaem) whenever the user comes back from another app's screen.
    var refresh by remember { mutableStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val notificationsOn = remember(refresh) { androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled() }
    val watches by produceState<List<String>?>(null, refresh) {
        value = try { Wearable.getNodeClient(context).connectedNodes.await().map { it.displayName } } catch (e: Exception) { emptyList() }
    }
    val speech by produceState<ModelHost.Status?>(null, refresh) { value = container.models.status() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
        )
    }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Section(R.string.settings_asr_section)
            ModeRow(AsrMode.IMMEDIATE, mode, R.string.mode_immediate, R.string.mode_immediate_desc) { scope.launch { settings.setAsrMode(it) } }
            ModeRow(AsrMode.SCHEDULED, mode, R.string.mode_scheduled, R.string.mode_scheduled_desc) { scope.launch { settings.setAsrMode(it) } }
            if (mode == AsrMode.SCHEDULED) {
                Row(Modifier.fillMaxWidth().padding(start = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { pickTime = true }) { Text(stringResource(R.string.schedule_time, formatMinutesOfDay(minutes))) }
                    Text(stringResource(R.string.charging_only), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = chargingOnly, onCheckedChange = { scope.launch { settings.setScheduleChargingOnly(it) } })
                }
            }
            ModeRow(AsrMode.IDLE, mode, R.string.mode_idle, R.string.mode_idle_desc) { scope.launch { settings.setAsrMode(it) } }
            if (mode == AsrMode.IDLE) {
                Row(Modifier.padding(start = 48.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IdlePreset.entries.forEach { p ->
                        FilterChip(selected = p == preset, onClick = { scope.launch { settings.setIdlePreset(p) } }, label = {
                            Text(stringResource(when (p) { IdlePreset.FAST -> R.string.idle_fast; IdlePreset.NORMAL -> R.string.idle_normal; IdlePreset.RELAXED -> R.string.idle_relaxed }))
                        })
                    }
                }
            }
            ModeRow(AsrMode.MANUAL, mode, R.string.mode_manual, R.string.mode_manual_desc) { scope.launch { settings.setAsrMode(it) } }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Section(R.string.settings_retention_section)
            Text(stringResource(R.string.retention_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SettingsStore.RETENTION_CHOICES.forEach { days ->
                Row(Modifier.fillMaxWidth().clickable { scope.launch { settings.setRetentionDays(days) } }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = retention == days, onClick = { scope.launch { settings.setRetentionDays(days) } })
                    Text(if (days == 0) stringResource(R.string.retention_off) else stringResource(R.string.retention_days, days))
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Section(R.string.settings_status_section)
            StatusRow(
                stringResource(R.string.status_notifications),
                stringResource(if (notificationsOn) R.string.status_on else R.string.status_off),
                if (notificationsOn) null else stringResource(R.string.action_open_settings),
            ) { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }
            StatusRow(
                stringResource(R.string.status_watch),
                watches?.let { if (it.isEmpty()) stringResource(R.string.status_watch_none) else it.joinToString() } ?: "…", null,
            ) {}
            val speechText = when (val s = speech) {
                null -> "…"
                ModelHost.Status.NotInstalled -> stringResource(R.string.status_speech_not_installed)
                ModelHost.Status.NotApproved -> stringResource(R.string.status_speech_not_approved)
                ModelHost.Status.NoModel -> stringResource(R.string.status_speech_no_model)
                is ModelHost.Status.Unavailable -> stringResource(R.string.status_speech_unavailable)
                is ModelHost.Status.Ready -> stringResource(R.string.status_speech_ready, s.modelId)
            }
            StatusRow(
                stringResource(R.string.status_speech), speechText,
                when (speech) {
                    ModelHost.Status.NotApproved -> stringResource(R.string.action_allow)
                    ModelHost.Status.NoModel, is ModelHost.Status.Unavailable -> stringResource(R.string.action_open_ondolsaem)
                    else -> null
                },
            ) {
                if (speech == ModelHost.Status.NotApproved) context.startActivity(OndolSaem.approvalIntent()) else launchOndolSaem(context)
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (pickTime) {
        val state = rememberTimePickerState(minutes / 60, minutes % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            text = { TimePicker(state) },
            confirmButton = { TextButton(onClick = { pickTime = false; scope.launch { settings.setScheduleMinutes(state.hour * 60 + state.minute) } }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Section(title: Int) {
    Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ModeRow(value: AsrMode, current: AsrMode, title: Int, desc: Int, onSelect: (AsrMode) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onSelect(value) }, verticalAlignment = Alignment.Top) {
        RadioButton(selected = current == value, onClick = { onSelect(value) })
        Column(Modifier.padding(top = 12.dp)) {
            Text(stringResource(title))
            Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, actionLabel: String?, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (actionLabel != null) OutlinedButton(onClick = onAction) { Text(actionLabel) }
    }
}
