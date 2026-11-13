package io.github.taehunkim.dorangori.wear.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import io.github.taehunkim.dorangori.wear.R
import io.github.taehunkim.dorangori.wear.recording.RecState
import io.github.taehunkim.dorangori.wear.recording.RecordingFiles
import io.github.taehunkim.dorangori.wear.recording.RecordingState
import io.github.taehunkim.dorangori.wear.recording.WatchSettings
import io.github.taehunkim.dorangori.wear.transfer.Transfer
import kotlinx.coroutines.delay

fun formatElapsed(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
fun RecorderScreen(onToggle: () -> Unit) {
    val context = LocalContext.current
    val state by RecordingState.state.collectAsStateWithLifecycle()
    val recording = state is RecState.Recording
    var pending by remember { mutableIntStateOf(0) }
    var autoStart by remember { mutableStateOf(WatchSettings.autoStart(context)) }
    // Recount after each state change: a recording that just finished is waiting to be sent.
    LaunchedEffect(state) { pending = RecordingFiles.pending(RecordingFiles.dir(context)).size }
    var phoneUp by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(state) { phoneUp = Transfer.phoneReachable(context) }
    val elapsed by produceState(0L, state) {
        val s = state
        if (s is RecState.Recording) {
            while (true) { value = SystemClock.elapsedRealtime() - s.startedAtElapsedMs; delay(250) }
        } else value = 0L
    }

    MaterialTheme {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item { Text(formatElapsed(elapsed), style = MaterialTheme.typography.displaySmall) }
            item {
                Button(onClick = onToggle) { Text(stringResource(if (recording) R.string.stop else R.string.record)) }
            }
            if (pending > 0) item {
                Text(stringResource(R.string.pending_count, pending), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            item {
                Text(
                    stringResource(when (phoneUp) { true -> R.string.phone_connected; false -> R.string.phone_not_found; null -> R.string.phone_checking }),
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                )
            }
            item {
                SwitchButton(
                    checked = autoStart,
                    onCheckedChange = { autoStart = it; WatchSettings.setAutoStart(context, it) },
                    label = { Text(stringResource(R.string.auto_start)) },
                )
            }
        }
    }
}
