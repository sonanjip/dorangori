package io.github.taehunkim.dorangori.wear.transfer

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import io.github.taehunkim.dorangori.wear.recording.RecordingFiles
import java.io.File

/** The phone acknowledges a recording by deleting its DataItem; the local file is no longer needed then. */
class AckListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        val dir = RecordingFiles.dir(this)
        for (event in events) {
            if (event.type != DataEvent.TYPE_DELETED) continue
            val name = DataPaths.nameOf(event.dataItem.uri.path.orEmpty()) ?: continue
            // The name comes from the data layer: keep it a plain file name inside the recordings folder.
            if (name.contains('/') || RecordingFiles.parse(name) == null) continue
            File(dir, name).delete()
        }
    }
}
