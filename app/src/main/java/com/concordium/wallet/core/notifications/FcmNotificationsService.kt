package com.concordium.wallet.core.notifications

import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.toBigInteger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

class FcmNotificationsService : FirebaseMessagingService() {
    private val serviceCoroutineContext = Dispatchers.IO + SupervisorJob()
    private val announcementNotificationManager: AnnouncementNotificationManager by lazy {
        AnnouncementNotificationManager(application)
    }
    private val transactionNotificationsManager: TransactionNotificationsManager by lazy {
        TransactionNotificationsManager(application)
    }
    private val accountRepository: AccountRepository by lazy {
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        AccountRepository(accountDao)
    }

    override fun onNewToken(token: String) {
        Log.d(
            "token_generated:" +
                    "\ntoken=$token"
        )
    }

    override fun onMessageReceived(message: RemoteMessage) = runBlocking(serviceCoroutineContext) {
        val notification: RemoteMessage.Notification? = message.notification
        val notificationTitle: String? = notification?.title
        val notificationBody: String? = notification?.body
        val dataType: String? = message.data["type"]

        Log.d(
            "message_received:" +
                    "\nmessageId=${message.messageId}" +
                    "\nnotificationTitle=${notificationTitle}," +
                    "\nnotificationBody=${notificationBody}," +
                    "\ndata=${message.data}"
        )

        when {
            // CCD transactions.
            dataType == TransactionNotificationsManager.TYPE_CCD_TX ->
                handleCcdTx(
                    data = message.data
                )

            // CIS2 transactions.
            dataType == TransactionNotificationsManager.TYPE_CIS2_TX ->
                handleCis2Tx(
                    data = message.data,
                )

            // Announcements.
            notificationBody != null ->
                handleAnnouncement(
                    notificationTitle = notificationTitle,
                    notificationBody = notificationBody,
                    messageId = message.messageId,
                )
        }

        Log.d(
            "message_handled:" +
                    "\nmessageId=${message.messageId}"
        )
    }

    private fun handleAnnouncement(
        notificationTitle: String?,
        notificationBody: String,
        messageId: String?,
    ) {
        Log.d(
            "handling_announcement:" +
                    "\nmessageId=$messageId"
        )

        announcementNotificationManager.notifyAnnouncement(
            title = notificationTitle,
            text = notificationBody,
            reference = messageId ?: System.currentTimeMillis(),
        )
    }

    private suspend fun handleCcdTx(
        data: Map<String, String>
    ) {
        Log.d("handling_ccd_tx")

        val amount = data["amount"]?.toBigInteger()
        if (amount == null) {
            Log.w("missing_amount")
            return
        }

        val recipientAccountAddress = data["recipient"]
        if (recipientAccountAddress == null) {
            Log.w("missing_recipient")
            return
        }
        val recipientAccount = accountRepository.findByAddress(recipientAccountAddress)
        if (recipientAccount == null) {
            Log.w(
                "recipient_account_not_found:" +
                        "\naddress=$recipientAccountAddress"
            )
            return
        }

        transactionNotificationsManager.notifyCcdTransaction(
            receivedAmount = amount,
            account = recipientAccount,
            reference = data["reference"] ?: System.currentTimeMillis(),
        )
    }

    private suspend fun handleCis2Tx(
        data: Map<String, String>
    ) {
        TODO()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceCoroutineContext.cancel()
    }
}
