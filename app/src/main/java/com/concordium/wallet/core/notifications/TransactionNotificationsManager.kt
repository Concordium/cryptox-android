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
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.preferences.NotificationsPreferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.util.Log
import java.math.BigInteger

class TransactionNotificationsManager(
    private val context: Context,
) {
    private val notificationsManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    private val notificationsPreferences: NotificationsPreferences by lazy {
        NotificationsPreferences(context)
    }

    private val areNotificationsEnabled: Boolean
        get() = notificationsManager.areNotificationsEnabled()

    private val areCcdTxNotificationsEnabled: Boolean
        get() = areNotificationsEnabled && notificationsPreferences.areCcdTxNotificationsEnabled

    private val areCis2TxNotificationsEnabled: Boolean
        get() = areNotificationsEnabled && notificationsPreferences.areCcdTxNotificationsEnabled

    @SuppressLint("MissingPermission")
    fun notifyCcdTransaction(
        receivedAmount: BigInteger,
        reference: Any,
        account: Account,
    ): Notification {
        ensureChannel()

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setDefaults(Notification.DEFAULT_ALL)
            .setContentTitle(
                context.getString(
                    R.string.template_transaction_received_notification_title,
                    context.getString(
                        R.string.amount,
                        CurrencyUtil.formatGTU(receivedAmount)
                    )
                )
            )
            .setContentText(context.getString(R.string.transaction_received_notification_text))
            // White icon is used for Android 5 compatibility.
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, AccountDetailsActivity::class.java)
                        .putExtra(AccountDetailsActivity.EXTRA_ACCOUNT, account)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
            .build()

        if (areCcdTxNotificationsEnabled) {
            notificationsManager.notify(reference.hashCode(), notification)
        } else {
            Log.d("Skip notify as disabled")
        }

        return notification
    }

    @SuppressLint("MissingPermission")
    fun notifyCis2Transaction(
        receivedAmount: BigInteger,
        token: Token,
        reference: Any,
        account: Account,
    ): Notification {
        ensureChannel()

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setDefaults(Notification.DEFAULT_ALL)
            .setContentTitle(
                context.getString(
                    R.string.template_transaction_received_notification_title,
                    "${CurrencyUtil.formatGTU(receivedAmount, token)} ${token.symbol}"
                )
            )
            .setContentText(context.getString(R.string.transaction_received_notification_text))
            // White icon is used for Android 5 compatibility.
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, AccountDetailsActivity::class.java)
                        .putExtra(AccountDetailsActivity.EXTRA_ACCOUNT, account)
                        .putExtra(AccountDetailsActivity.EXTRA_OPEN_TOKENS, true)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
            .build()

        if (areCis2TxNotificationsEnabled) {
            notificationsManager.notify(reference.hashCode(), notification)
        } else {
            Log.d("Skip notify as disabled")
        }

        return notification
    }

    private fun ensureChannel() {
        notificationsManager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_MAX
            )
                .setName(context.getString(R.string.transaction_notifications_channel_name))
                .build()
        )
    }

    companion object {
        private const val CHANNEL_ID = "transactions"
        const val TYPE_CCD_TX = "ccd-tx"
        const val TYPE_CIS2_TX = "cis2-tx"
    }
}