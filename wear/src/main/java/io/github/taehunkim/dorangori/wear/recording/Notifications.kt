package io.github.taehunkim.dorangori.wear.recording

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import io.github.taehunkim.dorangori.wear.MainActivity
import io.github.taehunkim.dorangori.wear.R

internal object Notifications {
    private const val CHANNEL = "recording"
    const val ID_RECORDING = 1

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.notif_channel_recording), NotificationManager.IMPORTANCE_LOW)
        )
    }

    /** The ongoing-recording notification, shown on the watch face as an Ongoing Activity. Tapping it opens the app without toggling. */
    fun recording(context: Context, startedAtWallMs: Long): android.app.Notification {
        ensureChannels(context)
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_OPEN).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle(context.getString(R.string.notif_recording_title))
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setWhen(startedAtWallMs)
            .setContentIntent(open)
        OngoingActivity.Builder(context, ID_RECORDING, builder)
            .setStaticIcon(R.drawable.ic_mic)
            .setTouchIntent(open)
            .setStatus(Status.Builder().addTemplate(context.getString(R.string.notif_recording_title)).build())
            .build()
            .apply(context)
        return builder.build()
    }
}
