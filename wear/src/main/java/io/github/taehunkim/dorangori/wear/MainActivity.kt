package io.github.taehunkim.dorangori.wear

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import io.github.taehunkim.dorangori.wear.recording.RecState
import io.github.taehunkim.dorangori.wear.recording.RecordingService
import io.github.taehunkim.dorangori.wear.recording.RecordingState
import io.github.taehunkim.dorangori.wear.recording.WatchSettings
import androidx.lifecycle.lifecycleScope
import io.github.taehunkim.dorangori.wear.transfer.Transfer
import io.github.taehunkim.dorangori.wear.ui.RecorderScreen
import kotlinx.coroutines.launch

/**
 * Hardware buttons are never intercepted. The user assigns a button to "open Dorangori" in the watch settings; opening the app
 * from the launcher then toggles recording: it starts one, or stops the one in progress.
 */
class MainActivity : ComponentActivity() {
    private var startAfterPermission = false

    private val askPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        val mic = granted[Manifest.permission.RECORD_AUDIO] == true
        if (mic && startAfterPermission) RecordingService.start(this)
        startAfterPermission = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RecorderScreen(onToggle = ::toggle) }
        // A rotation or process restore recreates the activity without a new launch: do not toggle again.
        if (savedInstanceState == null) onLaunched(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        onLaunched(intent)
    }

    override fun onResume() {
        super.onResume()
        // Anything still on the watch (phone was away, queue lost) is offered again; items that exist are skipped.
        lifecycleScope.launch { Transfer.syncPending(applicationContext) }
    }

    private fun onLaunched(intent: Intent?) {
        val fromLauncher = intent?.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
        if (!fromLauncher) return // the Ongoing Activity chip opens the app with ACTION_OPEN and must not stop the recording
        if (RecordingState.state.value is RecState.Recording) RecordingService.stop(this)
        else if (WatchSettings.autoStart(this)) startRecording()
    }

    private fun toggle() {
        if (RecordingState.state.value is RecState.Recording) RecordingService.stop(this) else startRecording()
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            RecordingService.start(this)
            return
        }
        startAfterPermission = true
        val wanted = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        askPermissions.launch(wanted.toTypedArray())
    }

    companion object {
        const val ACTION_OPEN = "io.github.taehunkim.dorangori.wear.action.OPEN"
    }
}
