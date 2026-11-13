package io.github.taehunkim.dorangori.notify

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import io.github.taehunkim.dorangori.DorangoriApp
import io.github.taehunkim.dorangori.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Notification buttons that need no screen: copy the text, start a transcription. Not exported. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, 0L)
        if (id <= 0) return
        val container = (context.applicationContext as DorangoriApp).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    ACTION_COPY -> container.dao.get(id)?.text?.takeIf { it.isNotBlank() }?.let { text ->
                        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Dorangori", text))
                        withContext(Dispatchers.Main) { Toast.makeText(context, R.string.toast_copied, Toast.LENGTH_SHORT).show() }
                    }
                    ACTION_TRANSCRIBE_NOW -> {
                        NotificationManagerCompat.from(context).cancel(100_000 + id.toInt())
                        container.scheduler.transcribeNow(id)
                    }
                    ACTION_SAVE_JOGAKBO -> container.jogakboSaver.enqueue(id)
                }
            } finally { pending.finish() }
        }
    }

    companion object {
        const val EXTRA_ID = "id"
        const val ACTION_COPY = "io.github.taehunkim.dorangori.action.COPY"
        const val ACTION_TRANSCRIBE_NOW = "io.github.taehunkim.dorangori.action.TRANSCRIBE_NOW"
        const val ACTION_SAVE_JOGAKBO = "io.github.taehunkim.dorangori.action.SAVE_JOGAKBO"
    }
}
