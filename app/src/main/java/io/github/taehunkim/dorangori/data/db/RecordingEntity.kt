package io.github.taehunkim.dorangori.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RecState {
    /** On the phone, not transcribed yet. */
    RECEIVED,
    TRANSCRIBING,
    DONE,
    FAILED,
    /** OndolSaem is missing, not approved, or has no speech model: transcription waits for the user to fix that. */
    NO_MODEL,
}

@Entity(tableName = "recordings", indices = [Index(value = ["fileName"], unique = true)])
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** `dorangori_yyyyMMdd_HHmmss.m4a`, also the file in `filesDir/recordings`. Unique: the watch may send a file twice. */
    val fileName: String,
    val recordedAt: Long,
    val receivedAt: Long,
    val durationMs: Long = 0,
    val state: RecState = RecState.RECEIVED,
    val text: String? = null,
    /** `[{"s":startMs,"e":endMs,"t":text}]`, kept so a note can carry timed lines. */
    val cuesJson: String? = null,
    /** The speech model that produced [text]; a different one later makes the text stale (re-transcribe). */
    val asrModelId: String? = null,
    val asrModelVersion: Int? = null,
    val error: String? = null,
    /** The Jogakbo note this recording was saved as, once it was. */
    val jogakboNoteId: Long? = null,
)
