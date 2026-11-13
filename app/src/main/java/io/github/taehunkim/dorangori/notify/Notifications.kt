package io.github.taehunkim.dorangori.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.taehunkim.dorangori.MainActivity
import io.github.taehunkim.dorangori.R
import io.github.taehunkim.dorangori.data.db.RecordingEntity
import io.github.taehunkim.dorangori.ml.ModelHost
import io.github.taehunkim.dorangori.ui.formatDuration
import io.github.taehunkim.dorangori.ui.formatWhen
import io.github.taehunkim.ondolsaem.sdk.OndolSaem

object Notifications {
    private const val CH_ARRIVAL = "arrival"
    private const val CH_RESULT = "result"
    private const val CH_PROGRESS = "progress"
    const val ID_PROGRESS = 1
    private const val ID_NEEDS_MODEL = 2
    private const val BASE_ARRIVAL = 100_000
    private const val BASE_RESULT = 200_000
    private const val BASE_SAVED = 300_000

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_ARRIVAL, context.getString(R.string.notif_channel_arrival), NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_RESULT, context.getString(R.string.notif_channel_result), NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CH_PROGRESS, context.getString(R.string.notif_channel_progress), NotificationManager.IMPORTANCE_LOW))
    }

    private fun canNotify(context: Context) = NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun flags() = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    /** Opens the recording's detail screen: this is what the notification's "review" tap does. */
    fun openIntent(context: Context, id: Long): PendingIntent =
        PendingIntent.getActivity(
            context, id.toInt(),
            Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_OPEN_RECORDING).putExtra(MainActivity.EXTRA_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            flags(),
        )

    private fun action(context: Context, id: Long, action: String, label: Int, slot: Int): NotificationCompat.Action {
        val pi = PendingIntent.getBroadcast(
            context, id.toInt() * 8 + slot,
            Intent(context, ActionReceiver::class.java).setAction(action).putExtra(ActionReceiver.EXTRA_ID, id), flags(),
        )
        return NotificationCompat.Action.Builder(0, context.getString(label), pi).build()
    }

    /** "New voice file arrived": when transcription does not start by itself. */
    fun arrived(context: Context, rec: RecordingEntity) {
        if (!canNotify(context)) return
        ensureChannels(context)
        val n = NotificationCompat.Builder(context, CH_ARRIVAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_arrived_title))
            .setContentText(context.getString(R.string.notif_arrived_text, formatWhen(rec.recordedAt)))
            .setAutoCancel(true)
            .setContentIntent(openIntent(context, rec.id))
            .addAction(action(context, rec.id, ActionReceiver.ACTION_TRANSCRIBE_NOW, R.string.action_transcribe_now, 0))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(BASE_ARRIVAL + rec.id.toInt(), n) }
    }

    /** The transcript: copy, ask Malbotdari, save to Jogakbo; tapping it opens the recording. */
    fun result(context: Context, rec: RecordingEntity) {
        if (!canNotify(context)) return
        ensureChannels(context)
        val nm = NotificationManagerCompat.from(context)
        nm.cancel(BASE_ARRIVAL + rec.id.toInt())
        val text = rec.text.orEmpty()
        val b = NotificationCompat.Builder(context, CH_RESULT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_done_title, formatWhen(rec.recordedAt)))
            .setAutoCancel(true)
            .setContentIntent(openIntent(context, rec.id))
        if (text.isBlank()) {
            b.setContentText(context.getString(R.string.notif_no_speech))
        } else {
            b.setContentText(text.lineSequence().first()).setStyle(NotificationCompat.BigTextStyle().bigText(text.take(MAX_BIG_TEXT)))
                .addAction(action(context, rec.id, ActionReceiver.ACTION_COPY, R.string.action_copy, 1))
            Share.askMalbotdariAction(context, text, rec.id)?.let { b.addAction(it) }
            b.addAction(action(context, rec.id, ActionReceiver.ACTION_SAVE_JOGAKBO, R.string.action_save_jogakbo, 3))
        }
        runCatching { nm.notify(BASE_RESULT + rec.id.toInt(), b.build()) }
    }

    /** Transcription cannot run until OndolSaem is installed, allowed, and has its speech model. */
    fun needsModel(context: Context, status: ModelHost.Status) {
        if (!canNotify(context)) return
        ensureChannels(context)
        val (target, text) = when (status) {
            ModelHost.Status.NotApproved -> OndolSaem.approvalIntent() to R.string.notif_needs_approval
            ModelHost.Status.NotInstalled -> null to R.string.notif_needs_install
            else -> context.packageManager.getLaunchIntentForPackage(OndolSaem.PACKAGE) to R.string.notif_needs_model
        }
        val b = NotificationCompat.Builder(context, CH_ARRIVAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_needs_title))
            .setContentText(context.getString(text))
            .setAutoCancel(true)
        target?.let { b.setContentIntent(PendingIntent.getActivity(context, 0, it, flags())) }
        runCatching { NotificationManagerCompat.from(context).notify(ID_NEEDS_MODEL, b.build()) }
    }

    /** The result of "save to Jogakbo": the note (tap to open it), or what stands in the way. */
    fun savedToJogakbo(context: Context, rec: RecordingEntity, outcome: JogakboSaver.Outcome) {
        if (!canNotify(context)) return
        ensureChannels(context)
        val (text, intent) = when (outcome) {
            is JogakboSaver.Outcome.Saved -> R.string.notif_jogakbo_saved to JogakboSaver.openNote(outcome.noteId)
            JogakboSaver.Outcome.NotInstalled -> R.string.notif_jogakbo_missing to null
            JogakboSaver.Outcome.NotApproved -> R.string.notif_jogakbo_approve to io.github.taehunkim.jogakbo.sdk.Jogakbo.approvalIntent()
            JogakboSaver.Outcome.TooOld -> R.string.notif_jogakbo_update to context.packageManager.getLaunchIntentForPackage(io.github.taehunkim.jogakbo.sdk.Jogakbo.PACKAGE)
            is JogakboSaver.Outcome.Failed -> R.string.notif_jogakbo_failed to null
        }
        val b = NotificationCompat.Builder(context, CH_ARRIVAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_jogakbo_title))
            .setContentText(context.getString(text))
            .setAutoCancel(true)
        intent?.let { b.setContentIntent(PendingIntent.getActivity(context, rec.id.toInt() * 8 + 4, it, flags())) }
        runCatching { NotificationManagerCompat.from(context).notify(BASE_SAVED + rec.id.toInt(), b.build()) }
    }

    fun progress(context: Context): android.app.Notification {
        ensureChannels(context)
        return NotificationCompat.Builder(context, CH_PROGRESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_progress_title))
            .setOngoing(true).setOnlyAlertOnce(true)
            .setProgress(0, 0, true)
            .build()
    }

    /** Transcription works again: the "speech recognition is not ready" notice is stale. */
    fun clearNeedsModel(context: Context) = NotificationManagerCompat.from(context).cancel(ID_NEEDS_MODEL)

    fun cancelFor(nm: NotificationManagerCompat, id: Long) {
        nm.cancel(BASE_ARRIVAL + id.toInt()); nm.cancel(BASE_RESULT + id.toInt()); nm.cancel(BASE_SAVED + id.toInt())
    }

    private const val MAX_BIG_TEXT = 4000
}
