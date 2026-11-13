package io.github.taehunkim.dorangori.transcribe

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import io.github.taehunkim.dorangori.DorangoriApp
import io.github.taehunkim.dorangori.notify.Notifications
import io.github.taehunkim.ondolsaem.sdk.IdleState
import io.github.taehunkim.ondolsaem.sdk.OndolSaem
import kotlinx.coroutines.flow.first

/**
 * Transcribes one recording ([KEY_ID]) or everything that waits (no id). With [KEY_IDLE_ONLY] it does nothing unless the
 * phone is idle, and stops as soon as the screen turns on.
 */
class TranscribeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val container get() = (applicationContext as DorangoriApp).container

    override suspend fun getForegroundInfo(): ForegroundInfo =
        ForegroundInfo(Notifications.ID_PROGRESS, Notifications.progress(applicationContext), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

    override suspend fun doWork(): Result {
        val idleOnly = inputData.getBoolean(KEY_IDLE_ONLY, false)
        val idle = IdleState(applicationContext)
        if (idleOnly && !idle.isIdle(container.settings.idlePreset.first())) return Result.success()
        val single = inputData.getLong(KEY_ID, 0L)
        val ids = if (single > 0) listOf(single) else container.dao.pendingIds()
        val priority = if (single > 0) OndolSaem.PRIORITY_INTERACTIVE else OndolSaem.PRIORITY_BACKGROUND
        for (id in ids) {
            if (idleOnly && idle.screenOn()) break
            container.transcriber.run(id, priority)
        }
        return Result.success()
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_IDLE_ONLY = "idle_only"
    }
}
