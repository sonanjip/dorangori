package io.github.taehunkim.dorangori.wear.recording

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

sealed interface RecState {
    data object Idle : RecState
    /** [startedAtElapsedMs] is on the `SystemClock.elapsedRealtime()` clock. */
    data class Recording(val file: File, val startedAtElapsedMs: Long) : RecState
}

/** Process-wide recording state shared by the service and the screen. */
object RecordingState {
    private val _state = MutableStateFlow<RecState>(RecState.Idle)
    val state: StateFlow<RecState> = _state

    private val _saved = MutableSharedFlow<File>(extraBufferCapacity = 16)
    /** Emits each finished recording once it is on disk (the transfer to the phone listens here). */
    val saved: SharedFlow<File> = _saved

    internal fun set(value: RecState) { _state.value = value }
    internal fun emitSaved(file: File) { _saved.tryEmit(file) }
}
