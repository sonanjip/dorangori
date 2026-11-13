package io.github.taehunkim.dorangori.ml

import android.content.Context
import io.github.taehunkim.dorangori.audio.Cue
import io.github.taehunkim.ondolsaem.sdk.HostCallException
import io.github.taehunkim.ondolsaem.sdk.HostUnavailableException
import io.github.taehunkim.ondolsaem.sdk.OndolSaem
import io.github.taehunkim.ondolsaem.sdk.OndolSaemClient
import java.io.File

/** The speech model, which runs in the OndolSaem app and is reached through its SDK. */
class ModelHost(context: Context) {
    val client = OndolSaemClient(context)

    sealed interface Status {
        data object NotInstalled : Status
        /** OndolSaem does not know this app yet: the user has to allow it there ([OndolSaem.approvalIntent]). */
        data object NotApproved : Status
        /** OndolSaem is there but has no downloaded speech model. */
        data object NoModel : Status
        data class Unavailable(val reason: String?) : Status
        data class Ready(val modelId: String, val version: Int) : Status
    }

    class NotReady(val status: Status) : Exception("speech recognition not ready: $status")

    class Result(val modelId: String, val version: Int, val cues: List<Cue>)

    suspend fun status(): Status {
        if (!client.isInstalled()) return Status.NotInstalled
        return try {
            val info = client.activeModel(OndolSaem.Categories.ASR)
            if (info == null || !info.ready || info.id.isEmpty() || !client.canTranscribe()) Status.NoModel
            else Status.Ready(info.id, info.version)
        } catch (e: HostUnavailableException) {
            when (e.reason) {
                HostUnavailableException.Reason.NOT_INSTALLED -> Status.NotInstalled
                HostUnavailableException.Reason.NOT_APPROVED, HostUnavailableException.Reason.NOT_ALLOWED -> Status.NotApproved
                else -> Status.Unavailable(e.message)
            }
        } catch (e: HostCallException) {
            Status.NoModel
        }
    }

    /** [pcm]: raw 16 kHz mono 16-bit PCM. Keeps the speech model loaded for the length of the call. */
    suspend fun transcribe(pcm: File, priority: Int): Result {
        val ready = status().let { it as? Status.Ready ?: throw NotReady(it) }
        val cues = ArrayList<Cue>()
        client.acquire(OndolSaem.Categories.ASR).use {
            client.transcribe(pcm, 0L, priority) { s, e, t -> cues += Cue(s, e, t) }
        }
        return Result(ready.modelId, ready.version, cues)
    }
}
