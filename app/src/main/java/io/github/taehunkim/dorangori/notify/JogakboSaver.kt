package io.github.taehunkim.dorangori.notify

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.taehunkim.dorangori.DorangoriApp
import io.github.taehunkim.dorangori.R
import io.github.taehunkim.dorangori.data.db.RecordingEntity
import io.github.taehunkim.dorangori.transcribe.Transcript
import io.github.taehunkim.dorangori.ui.formatWhen
import io.github.taehunkim.jogakbo.sdk.Jogakbo
import io.github.taehunkim.jogakbo.sdk.JogakboCallException
import io.github.taehunkim.jogakbo.sdk.JogakboClient
import io.github.taehunkim.jogakbo.sdk.JogakboUnavailableException
import io.github.taehunkim.jogakbo.sdk.NewAudioNote
import io.github.taehunkim.jogakbo.sdk.NoteCue

/** Saves a recording to Jogakbo as one note: the audio and its transcript together (Jogakbo SDK API 2). */
class JogakboSaver(private val context: Context) {
    val client = JogakboClient(context)

    fun enqueue(id: Long) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "save-jogakbo-$id", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<SaveWorker>().setInputData(workDataOf(SaveWorker.KEY_ID to id)).build(),
        )
    }

    sealed interface Outcome {
        data class Saved(val noteId: Long) : Outcome
        data object NotInstalled : Outcome
        data object NotApproved : Outcome
        data object TooOld : Outcome
        data class Failed(val message: String?) : Outcome
    }

    /** The note id is remembered on the recording, so saving twice opens the same note instead of making a copy. */
    suspend fun save(rec: RecordingEntity): Outcome {
        rec.jogakboNoteId?.let { return Outcome.Saved(it) }
        val container = (context.applicationContext as DorangoriApp).container
        val audio = container.repo.audioFile(rec)
        if (!audio.exists()) return Outcome.Failed("audio file missing")
        return try {
            val cues = Transcript.fromJson(rec.cuesJson).map { NoteCue(it.startMs, it.endMs, it.text) }
            val title = context.getString(R.string.jogakbo_note_title, formatWhen(rec.recordedAt))
            val id = client.createAudioNote(audio, NewAudioNote(title, cues, rec.durationMs, rec.recordedAt, origin = "dorangori"))
            container.dao.setJogakboNote(rec.id, id)
            Outcome.Saved(id)
        } catch (e: JogakboUnavailableException) {
            when (e.reason) {
                JogakboUnavailableException.Reason.NOT_INSTALLED -> Outcome.NotInstalled
                JogakboUnavailableException.Reason.NOT_APPROVED, JogakboUnavailableException.Reason.NOT_ALLOWED -> Outcome.NotApproved
                JogakboUnavailableException.Reason.HOST_TOO_OLD -> Outcome.TooOld
                else -> Outcome.Failed(e.message)
            }
        } catch (e: JogakboCallException) {
            Outcome.Failed(e.message)
        }
    }

    class SaveWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val container = (applicationContext as DorangoriApp).container
            val rec = container.dao.get(inputData.getLong(KEY_ID, 0L)) ?: return Result.success()
            val outcome = container.jogakboSaver.save(rec)
            Notifications.savedToJogakbo(applicationContext, rec, outcome)
            return Result.success()
        }

        companion object { const val KEY_ID = "id" }
    }

    companion object {
        /** An intent for the user to look at what was saved, or to fix what blocked it. */
        fun openNote(noteId: Long) = Jogakbo.openNoteIntent(noteId)
    }
}
