package io.github.taehunkim.dorangori.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.taehunkim.dorangori.DorangoriApp
import io.github.taehunkim.dorangori.Storage
import io.github.taehunkim.dorangori.watch.DataPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * `adb shell am broadcast -a io.github.taehunkim.dorangori.debug.INGEST -n .../.debug.DebugIngestReceiver --es path <file> [--el recordedAt <ms>]`
 * where <file> is a `dorangori_yyyyMMdd_HHmmss.m4a` readable by the app (for example under /sdcard/Android/data/<id>/files/).
 */
class DebugIngestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val src = File(intent.getStringExtra("path") ?: return)
        val name = src.name
        if (!DataPaths.isValidName(name) || !src.exists()) { Log.w("DebugIngest", "bad file $src"); return }
        val container = (context.applicationContext as DorangoriApp).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (container.dao.getByName(name) == null) {
                    src.copyTo(File(Storage.dir(context), name), overwrite = true)
                    val id = container.repo.register(name, intent.getLongExtra("recordedAt", System.currentTimeMillis()))
                    Log.i("DebugIngest", "registered $name as $id")
                    if (id != null) container.scheduler.onArrived(id)
                }
            } finally { pending.finish() }
        }
    }
}
