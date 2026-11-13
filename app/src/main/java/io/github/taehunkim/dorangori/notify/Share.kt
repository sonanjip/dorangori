package io.github.taehunkim.dorangori.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.github.taehunkim.dorangori.R

object Share {
    const val MALBOTDARI = "io.github.taehunkim.malbotdari"

    /**
     * Opens a new Malbotdari chat with [text] in the input box (the user presses send). Malbotdari takes `ACTION_SEND text/plain`.
     * Null when Malbotdari is not installed.
     */
    fun askMalbotdari(context: Context, text: String): Intent? {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
            .setPackage(MALBOTDARI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent.takeIf { context.packageManager.resolveActivity(it, 0) != null }
    }

    /** A notification action that does the same without going through this app first (Android 12 blocks activity starts from receivers). */
    fun askMalbotdariAction(context: Context, text: String, id: Long): NotificationCompat.Action? {
        val intent = askMalbotdari(context, text) ?: return null
        val pi = PendingIntent.getActivity(context, id.toInt() * 8 + 2, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action.Builder(0, context.getString(R.string.action_ask_malbotdari), pi).build()
    }
}
