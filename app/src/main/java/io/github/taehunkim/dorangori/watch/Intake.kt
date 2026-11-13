package io.github.taehunkim.dorangori.watch

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import io.github.taehunkim.dorangori.DorangoriApp
import io.github.taehunkim.dorangori.Storage
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Takes a recording from the watch: file to disk, row to the database, then the DataItem is deleted, which is the
 * acknowledgement that lets the watch drop its copy. Blocking: call it from a background thread.
 */
object Intake {
    private const val TAG = "Intake"

    /** Returns the new recording's id, or null when nothing new came in (repeat, bad name, or a failed copy that will be retried). */
    fun ingest(context: Context, item: DataItem): Long? {
        val app = context.applicationContext
        val name = DataPaths.nameOf(item.uri.path.orEmpty()) ?: return null
        val container = (app as DorangoriApp).container
        val dataClient = Wearable.getDataClient(app)
        val map = DataMapItem.fromDataItem(item).dataMap
        val known = runBlocking { container.dao.getByName(name) } != null
        var newId: Long? = null
        if (!known) {
            val asset = map.getAsset(DataPaths.KEY_AUDIO) ?: return null
            val recordedAt = map.getLong(DataPaths.KEY_RECORDED_AT, System.currentTimeMillis())
            val dir = Storage.dir(app)
            val tmp = File(dir, "$name.incoming")
            try {
                val response = Tasks.await(dataClient.getFdForAsset(asset))
                response.inputStream.use { input -> tmp.outputStream().use { out -> input.copyTo(out) } }
                if (!tmp.renameTo(File(dir, name))) throw java.io.IOException("cannot move $name into place")
            } catch (e: Exception) {
                // Not acknowledged: the item stays queued and the next pull() takes it again.
                Log.w(TAG, "cannot receive $name", e)
                tmp.delete()
                return null
            }
            newId = runBlocking { container.repo.register(name, recordedAt) }
        }
        try { Tasks.await(dataClient.deleteDataItems(item.uri)) } catch (e: Exception) { Log.w(TAG, "cannot acknowledge $name", e) }
        return newId
    }

    /** Takes whatever is still queued (for example after an item was missed): called when the app starts. */
    fun pull(context: Context): List<Long> {
        val app = context.applicationContext
        val ids = ArrayList<Long>()
        try {
            val buffer = Tasks.await(Wearable.getDataClient(app).getDataItems(Uri.Builder().scheme("wear").path(DataPaths.RECORDING_PREFIX).build(), DataClient.FILTER_PREFIX))
            try { buffer.forEach { ingest(app, it.freeze())?.let(ids::add) } } finally { buffer.release() }
        } catch (e: Exception) {
            Log.w(TAG, "cannot pull queued recordings", e)
        }
        ids.forEach { id -> (app as DorangoriApp).container.scheduler.onArrivedBlocking(id) }
        return ids
    }
}
