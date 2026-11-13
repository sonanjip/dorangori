package io.github.taehunkim.dorangori.wear.recording

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.app.ServiceCompat
import java.io.File
import java.time.Instant

/**
 * Records while the screen may be off. It is started by the screen while visible (Android 14 does not let a service start the
 * microphone from the background), which is why the permission is asked for there and only checked here.
 */
class RecordingService : Service() {
    private var recorder: Recorder? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
            else -> if (RecordingState.state.value !is RecState.Recording) stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun start() {
        if (RecordingState.state.value is RecState.Recording) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }
        val now = Instant.now()
        ServiceCompat.startForeground(
            this, Notifications.ID_RECORDING, Notifications.recording(this, now.toEpochMilli()),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
        )
        val dir = RecordingFiles.dir(this)
        RecordingFiles.deleteStaleParts(dir)
        val final = File(dir, RecordingFiles.uniqueName(dir, now))
        val rec = Recorder(this)
        try {
            rec.start(RecordingFiles.partFor(final), WatchSettings.maxMinutes(this) * 60_000) { stop() }
        } catch (_: Exception) {
            RecordingFiles.partFor(final).delete()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        recorder = rec
        RecordingState.set(RecState.Recording(final, SystemClock.elapsedRealtime()))
    }

    private fun stop() {
        val current = RecordingState.state.value as? RecState.Recording
        if (current == null) { stopSelf(); return }
        val ok = recorder?.stop() ?: false
        recorder = null
        val part = RecordingFiles.partFor(current.file)
        val long = SystemClock.elapsedRealtime() - current.startedAtElapsedMs >= MIN_DURATION_MS
        if (ok && long && part.renameTo(current.file)) {
            RecordingState.set(RecState.Idle)
            RecordingState.emitSaved(current.file)
        } else {
            part.delete()
            RecordingState.set(RecState.Idle)
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (RecordingState.state.value is RecState.Recording) stop()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "io.github.taehunkim.dorangori.wear.action.START"
        const val ACTION_STOP = "io.github.taehunkim.dorangori.wear.action.STOP"
        /** Anything shorter is a mis-tap, not a recording. */
        const val MIN_DURATION_MS = 1_000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, RecordingService::class.java).setAction(ACTION_START))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, RecordingService::class.java).setAction(ACTION_STOP))
        }
    }
}
