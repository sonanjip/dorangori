package io.github.taehunkim.dorangori.retention

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.taehunkim.dorangori.DorangoriApp
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** Deletes recordings (audio, text and list entry) older than the retention setting. */
class RetentionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as DorangoriApp).container
        val cutoff = Retention.cutoffMs(System.currentTimeMillis(), container.settings.retentionDays.first()) ?: return Result.success()
        container.repo.delete(container.dao.olderThan(cutoff).map { it.id })
        return Result.success()
    }

    companion object {
        private const val PERIODIC = "retention-daily"
        private const val NOW = "retention-now"

        /** Keeps one daily run while retention is on, and runs once right away so a new setting takes effect at once. */
        fun apply(context: Context, days: Int) {
            val wm = WorkManager.getInstance(context.applicationContext)
            if (days <= 0) { wm.cancelUniqueWork(PERIODIC); return }
            wm.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<RetentionWorker>(1, TimeUnit.DAYS).build())
            wm.enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<RetentionWorker>().build())
        }
    }
}
