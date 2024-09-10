package com.concordium.wallet.core.notifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.concordium.wallet.R
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.util.Log

class AnnouncementNotificationManager(
    private val context: Context,
) {
    private val notificationsManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    private val areNotificationsEnabled: Boolean
        get() = notificationsManager.areNotificationsEnabled()

    @SuppressLint("MissingPermission")
    fun notifyAnnouncement(
        title: String?,
        text: String,
        reference: Any,
    ): Notification {
        ensureChannel()

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setDefaults(Notification.DEFAULT_ALL)
            .setContentTitle(title)
            .setContentText(text)
            // White icon is used for Android 5 compatibility.
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
            .build()

        if (areNotificationsEnabled) {
            notificationsManager.notify(reference.hashCode(), notification)
        } else {
            Log.d("Skip notify as disabled")
        }

        return notification
    }

    fun ensureChannel() {
        notificationsManager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_DEFAULT
            )
                .setName(context.getString(R.string.announcements_notification_channel_name))
                .build()
        )
    }

    private companion object {
        private const val CHANNEL_ID = "announcements"
    }
}
