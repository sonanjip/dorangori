package io.github.taehunkim.dorangori.transcribe

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.github.taehunkim.dorangori.data.db.RecordingDao
import io.github.taehunkim.dorangori.data.prefs.AsrMode
import io.github.taehunkim.dorangori.data.prefs.SettingsStore
import io.github.taehunkim.dorangori.notify.Notifications
import io.github.taehunkim.dorangori.retention.Timing
import io.github.taehunkim.ondolsaem.sdk.IdlePreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

/**
 * Decides when received recordings are transcribed (the setting [AsrMode]): right away, once a day, when idle, or only on request.
 * [apply] is idempotent, so it can run whenever the settings change.
 */
class TranscribeScheduler(private val context: Context, private val settings: SettingsStore, private val dao: RecordingDao) {
    private val work = WorkManager.getInstance(context.applicationContext)

    private data class Config(val mode: AsrMode, val minutes: Int, val chargingOnly: Boolean, val preset: IdlePreset)

    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(settings.asrMode, settings.scheduleMinutes, settings.scheduleChargingOnly, settings.idlePreset, ::Config)
                .distinctUntilChanged().collect { apply(it) }
        }
    }

    private fun apply(cfg: Config) {
        work.cancelUniqueWork(SCHEDULED)
        work.cancelUniqueWork(IDLE)
        when (cfg.mode) {
            AsrMode.IMMEDIATE -> work.enqueueUniqueWork(CATCH_UP, ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<TranscribeWorker>().build())
            AsrMode.SCHEDULED -> {
                val constraints = Constraints.Builder().setRequiresBatteryNotLow(true).setRequiresCharging(cfg.chargingOnly).build()
                val delay = Timing.delayUntilNext(cfg.minutes)
                work.enqueueUniquePeriodicWork(
                    SCHEDULED, ExistingPeriodicWorkPolicy.UPDATE,
                    PeriodicWorkRequestBuilder<TranscribeWorker>(24, TimeUnit.HOURS).setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES).setConstraints(constraints).build(),
                )
            }
            AsrMode.IDLE -> work.enqueueUniquePeriodicWork(
                IDLE, ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<TranscribeWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(Constraints.Builder().setRequiresCharging(true).build())
                    .setInputData(workDataOf(TranscribeWorker.KEY_IDLE_ONLY to true)).build(),
            )
            AsrMode.MANUAL -> Unit
        }
    }

    /** A recording just arrived: transcribe it now, or only announce it, depending on the mode. */
    suspend fun onArrived(id: Long) {
        if (settings.asrModeNow() == AsrMode.IMMEDIATE) transcribeNow(id)
        else dao.get(id)?.let { Notifications.arrived(context, it) }
    }

    /** For callers on a background thread (the Data Layer listener). */
    fun onArrivedBlocking(id: Long) = runBlocking { onArrived(id) }

    /** "Transcribe now": one recording, ahead of other work. */
    fun transcribeNow(id: Long) {
        work.enqueueUniqueWork(
            "transcribe-$id", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<TranscribeWorker>()
                .setInputData(workDataOf(TranscribeWorker.KEY_ID to id))
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 2, TimeUnit.MINUTES)
                .build(),
        )
    }

    /** OndolSaem got (or lost) a model: recordings that waited for it are tried again when the mode is immediate. */
    suspend fun onModelChanged() {
        if (settings.asrModeNow() == AsrMode.IMMEDIATE) {
            work.enqueueUniqueWork(CATCH_UP, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<TranscribeWorker>().build())
        }
    }

    companion object {
        private const val CATCH_UP = "transcribe-catch-up"
        private const val SCHEDULED = "transcribe-scheduled"
        private const val IDLE = "transcribe-idle"
    }
}
