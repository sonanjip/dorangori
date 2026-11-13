package io.github.taehunkim.dorangori.transcribe

import android.content.Context
import android.net.Uri
import io.github.taehunkim.dorangori.Storage
import io.github.taehunkim.dorangori.audio.AudioDecoder
import io.github.taehunkim.dorangori.data.db.RecState
import io.github.taehunkim.dorangori.data.db.RecordingDao
import io.github.taehunkim.dorangori.ml.ModelHost
import io.github.taehunkim.dorangori.notify.Notifications
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.io.File

/** Turns one received recording into text: decode to PCM, recognise in OndolSaem, store, notify. */
class Transcriber(private val context: Context, private val dao: RecordingDao, private val models: ModelHost, private val settings: io.github.taehunkim.dorangori.data.prefs.SettingsStore) {
    suspend fun run(id: Long, priority: Int) {
        val rec = dao.get(id) ?: return
        if (rec.state == RecState.DONE) return
        dao.setState(id, RecState.TRANSCRIBING, null)
        val audio = File(Storage.dir(context), rec.fileName)
        if (!audio.exists()) { dao.setState(id, RecState.FAILED, "audio file missing"); return }
        val pcm = File(context.cacheDir, "asr-$id.pcm")
        try {
            dao.setDuration(id, AudioDecoder.decode(context, Uri.fromFile(audio), pcm))
            // LocaleManager.systemLocales deliberately bypasses per-app display-language overrides.
            val systemLocale = if (android.os.Build.VERSION.SDK_INT >= 33) {
                context.getSystemService(android.app.LocaleManager::class.java).systemLocales[0]
            } else android.content.res.Resources.getSystem().configuration.locales[0]
            val language = settings.asrLanguage.first().resolve(systemLocale)
            val result = models.transcribe(pcm, priority, language)
            dao.setResult(id, Transcript.join(result.cues), Transcript.toJson(result.cues), result.modelId, result.version)
            Notifications.clearNeedsModel(context)
            dao.get(id)?.let { Notifications.result(context, it) }
        } catch (e: ModelHost.LanguageRequiresUpdate) {
            dao.setState(id, RecState.FAILED, context.getString(io.github.taehunkim.dorangori.R.string.asr_update_required))
        } catch (e: ModelHost.NotReady) {
            dao.setState(id, RecState.NO_MODEL, e.status.javaClass.simpleName)
            Notifications.needsModel(context, e.status)
        } catch (e: CancellationException) {
            dao.setState(id, RecState.RECEIVED, null)
            throw e
        } catch (e: Exception) {
            dao.setState(id, RecState.FAILED, e.message ?: e.javaClass.simpleName)
        } finally {
            pcm.delete()
        }
    }
}
