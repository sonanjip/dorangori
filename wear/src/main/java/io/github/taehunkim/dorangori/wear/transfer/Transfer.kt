package io.github.taehunkim.dorangori.wear.transfer

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import io.github.taehunkim.dorangori.wear.recording.RecordingFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Hands finished recordings to the phone through the Wearable Data Layer. Each recording is one DataItem at
 * `/rec/<file name>` holding the audio as an Asset; the Data Layer keeps it queued until the phone is reachable.
 * The phone deletes the DataItem once it has the file, and [AckListenerService] then removes the local copy.
 */
object Transfer {
    private const val TAG = "Transfer"

    /**
     * Puts every finished recording that has no DataItem yet. Safe to call at any time: the file name is the key, so a
     * recording whose item exists is skipped. Returns how many were queued.
     */
    suspend fun syncPending(context: Context): Int = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val files = RecordingFiles.pending(RecordingFiles.dir(app))
        if (files.isEmpty()) return@withContext 0
        val client = Wearable.getDataClient(app)
        val existing = try {
            val buffer = Tasks.await(client.getDataItems(Uri.Builder().scheme("wear").path(DataPaths.RECORDING_PREFIX).build(), com.google.android.gms.wearable.DataClient.FILTER_PREFIX))
            try { buffer.mapNotNull { DataPaths.nameOf(it.uri.path.orEmpty()) }.toSet() } finally { buffer.release() }
        } catch (e: Exception) {
            Log.w(TAG, "cannot list data items", e); emptySet()
        }
        var queued = 0
        for (file in files) {
            if (file.name in existing) continue
            try { put(client, file); queued++ } catch (e: Exception) { Log.w(TAG, "cannot queue ${file.name}", e) }
        }
        queued
    }

    private fun put(client: com.google.android.gms.wearable.DataClient, file: File) {
        val recordedAt = RecordingFiles.parse(file.name)?.toEpochMilli() ?: file.lastModified()
        val request = PutDataMapRequest.create(DataPaths.pathFor(file.name)).apply {
            dataMap.putAsset(DataPaths.KEY_AUDIO, Asset.createFromBytes(file.readBytes()))
            dataMap.putLong(DataPaths.KEY_RECORDED_AT, recordedAt)
            dataMap.putString(DataPaths.KEY_NAME, file.name)
        }.asPutDataRequest().setUrgent()
        Tasks.await(client.putDataItem(request))
    }

    /** True when the phone app is installed on a reachable phone. */
    suspend fun phoneReachable(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val info = Tasks.await(Wearable.getCapabilityClient(context).getCapability(DataPaths.CAPABILITY_PHONE, com.google.android.gms.wearable.CapabilityClient.FILTER_REACHABLE))
            info.nodes.isNotEmpty()
        } catch (e: Exception) { false }
    }
}
