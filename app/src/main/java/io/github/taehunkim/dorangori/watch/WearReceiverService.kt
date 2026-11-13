package io.github.taehunkim.dorangori.watch

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import io.github.taehunkim.dorangori.DorangoriApp

/** Receives recordings sent by the watch, also when the app is not running. */
class WearReceiverService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        val items = events.filter { it.type == DataEvent.TYPE_CHANGED }.map { it.dataItem.freeze() }
        val scheduler = (application as DorangoriApp).container.scheduler
        for (item in items) Intake.ingest(this, item)?.let { scheduler.onArrivedBlocking(it) }
    }
}
