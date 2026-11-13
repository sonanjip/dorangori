package io.github.taehunkim.dorangori.wear.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** AAC in m4a, 16 kHz mono, 32 kbps (about 0.24 MB a minute). The phone decodes it to PCM for speech recognition. */
class Recorder(private val context: Context) {
    private var recorder: MediaRecorder? = null

    fun start(target: File, maxDurationMs: Int, onMaxDuration: () -> Unit) {
        @Suppress("DEPRECATION")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioChannels(1)
            r.setAudioSamplingRate(SAMPLE_RATE)
            r.setAudioEncodingBitRate(BIT_RATE)
            r.setMaxDuration(maxDurationMs)
            r.setOutputFile(target.absolutePath)
            r.setOnInfoListener { _, what, _ -> if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) onMaxDuration() }
            r.prepare()
            r.start()
        } catch (e: Exception) {
            r.release()
            throw e
        }
        recorder = r
    }

    /** Stops and releases. False when the recorder produced no valid file (stopped too soon, or failed). */
    fun stop(): Boolean {
        val r = recorder ?: return false
        recorder = null
        return try { r.stop(); true } catch (_: RuntimeException) { false } finally { r.release() }
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        const val BIT_RATE = 32_000
    }
}
