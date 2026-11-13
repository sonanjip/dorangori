package io.github.taehunkim.dorangori.data

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import io.github.taehunkim.dorangori.Storage
import io.github.taehunkim.dorangori.data.db.RecordingDao
import io.github.taehunkim.dorangori.data.db.RecordingEntity
import io.github.taehunkim.dorangori.notify.Notifications
import java.io.File

class RecordingRepository(private val context: Context, private val dao: RecordingDao) {
    fun audioFile(rec: RecordingEntity): File = File(Storage.dir(context), rec.fileName)

    /** Registers a file that is already in the recordings folder. Null when the name is known (a repeated delivery). */
    suspend fun register(fileName: String, recordedAt: Long, nowMs: Long = System.currentTimeMillis()): Long? {
        val id = dao.insert(RecordingEntity(fileName = fileName, recordedAt = recordedAt, receivedAt = nowMs))
        return id.takeIf { it > 0 }
    }

    /** Deletes the rows and their audio files, and any notification that still shows them. */
    suspend fun delete(ids: List<Long>) {
        if (ids.isEmpty()) return
        dao.getAll(ids).forEach { audioFile(it).delete() }
        dao.delete(ids)
        val nm = NotificationManagerCompat.from(context)
        ids.forEach { Notifications.cancelFor(nm, it) }
    }
}
