package com.concordium.wallet.core.notifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.data.model.ContractToken
import com.concordium.wallet.data.model.ProtocolLevelToken
import com.concordium.wallet.data.preferences.WalletNotificationsPreferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.util.Log
import java.math.BigInteger

class TransactionNotificationsManager(
    private val context: Context,
) {
    private val notificationsManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    private val walletNotificationsPreferences: WalletNotificationsPreferences by lazy {
        App.appCore.session.walletStorage.notificationsPreferences
    }

    private val areNotificationsEnabled: Boolean
        get() = notificationsManager.areNotificationsEnabled()

    private val areCcdTxNotificationsEnabled: Boolean
        get() = areNotificationsEnabled && walletNotificationsPreferences.areCcdTxNotificationsEnabled

    private val areCis2TxNotificationsEnabled: Boolean
        get() = areNotificationsEnabled && walletNotificationsPreferences.areCis2TxNotificationsEnabled

    private val arePltTxNotificationsEnabled: Boolean
        get() = areNotificationsEnabled && walletNotificationsPreferences.arePltTxNotificationsEnabled

    fun notifyCcdTransaction(
        receivedAmount: BigInteger,
        reference: Any,
        account: Account,
    ): Notification {
        val title = context.getString(
            R.string.template_transaction_received_notification_title,
            context.getString(R.string.amount, CurrencyUtil.formatGTU(receivedAmount))
        )
        val notification = buildNotification(title, account)
        return showNotificationIfEnabled(notification, reference, areCcdTxNotificationsEnabled)
    }

    fun notifyCis2Transaction(
        receivedAmount: BigInteger,
        token: ContractToken,
        reference: Any,
        account: Account,
    ): Notification {
        val title = context.getString(
            R.string.template_transaction_received_notification_title,
            "${CurrencyUtil.formatGTU(receivedAmount, token)} ${token.symbol}"
        )
        val notification = buildNotification(title, account) {
            if (!token.isNewlyReceived) {
                putExtra(MainActivity.EXTRA_NOTIFICATION_TOKEN_ID, token.uid)
            }
        }
        return showNotificationIfEnabled(notification, reference, areCis2TxNotificationsEnabled)
    }

    fun notifyPltTransaction(
        receivedAmount: BigInteger,
        token: ProtocolLevelToken,
        reference: Any,
        account: Account
    ): Notification {
        val title = context.getString(
            R.string.template_transaction_received_notification_title,
            "${CurrencyUtil.formatGTU(receivedAmount, token)} ${token.symbol}"
        )
        val notification = buildNotification(title, account) {
            if (!token.isNewlyReceived) {
                putExtra(MainActivity.EXTRA_NOTIFICATION_TOKEN_ID, token.tokenId)
            }
        }
        return showNotificationIfEnabled(notification, reference, arePltTxNotificationsEnabled)
    }

    private fun buildNotification(
        title: String,
        account: Account,
        intentExtras: Intent.() -> Unit = {}
    ): Notification {
        ensureChannel()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setDefaults(Notification.DEFAULT_ALL)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.transaction_received_notification_text))
            .setSmallIcon(R.drawable.ic_notification) // White icon for Android 5
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(createPendingIntent(account, intentExtras))
            .build()
    }

    private fun createPendingIntent(
        account: Account,
        intentExtras: Intent.() -> Unit
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_ACTIVATE_ACCOUNT, true)
            .putExtra(MainActivity.EXTRA_ACCOUNT_ADDRESS, account.address)
            .apply(intentExtras)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @SuppressLint("MissingPermission")
    private fun showNotificationIfEnabled(
        notification: Notification,
        reference: Any,
        isEnabled: Boolean
    ): Notification {
        if (isEnabled) {
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
        const val TYPE_PLT_TX = "plt-tx"
    }
}
