package io.github.taehunkim.dorangori.ui.detail

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.io.File

/** Plays one recording; [positionMs] follows the playback while it runs. */
class PlayerState internal constructor(private val file: File) {
    private var player: MediaPlayer? = null
    var playing by mutableStateOf(false); private set
    var positionMs by mutableIntStateOf(0); private set
    var durationMs by mutableIntStateOf(0); private set

    internal fun poll() { player?.let { if (playing) positionMs = it.currentPosition } }

    fun toggle() {
        val p = player ?: MediaPlayer().apply {
            setDataSource(file.absolutePath); prepare()
            setOnCompletionListener { this@PlayerState.playing = false; this@PlayerState.positionMs = 0; seekTo(0) }
            durationMs = duration
        }.also { player = it }
        if (p.isPlaying) { p.pause(); playing = false } else { p.start(); playing = true }
    }

    fun seekTo(ms: Int) { player?.seekTo(ms); positionMs = ms }

    internal fun release() { player?.release(); player = null; playing = false }
}

@Composable
fun rememberPlayer(file: File): PlayerState {
    val state = remember(file) { PlayerState(file) }
    DisposableEffect(state) { onDispose { state.release() } }
    LaunchedEffect(state, state.playing) { while (state.playing) { state.poll(); delay(200) } }
    return state
}
