package com.concordium.wallet.core.notifications

import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.data.model.UrlHolder
import com.concordium.wallet.data.room.ContractToken
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
    private val contractTokensRepository: ContractTokensRepository by lazy {
        val contractTokenDao = WalletDatabase.getDatabase(application).contractTokenDao()
        ContractTokensRepository(contractTokenDao)
    }
    private val updateNotificationsSubscriptionUseCase by lazy {
        UpdateNotificationsSubscriptionUseCase(application)
    }

    override fun onNewToken(token: String) = runBlocking(serviceCoroutineContext) {
        Log.d(
            "updating_subscriptions_with_new_token:" +
                    "\ntoken=$token"
        )

        try {
            updateNotificationsSubscriptionUseCase()
        } catch (error: Exception){
            Log.e(
                "failed_updating_subscriptions",
                error
            )
        }
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

        try {
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

                else ->
                    error("Unsupported message")
            }

            Log.d(
                "message_handled:" +
                        "\nmessageId=${message.messageId}"
            )
        } catch (error: Exception) {
            Log.e(
                "failed_handling_message:" +
                        "\nmessageId=${message.messageId}",
                error
            )
        }
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

    /**
     * @param data filled according to the
     * [specification](https://concordium.atlassian.net/wiki/spaces/EN/pages/1502019590/Message+data+payload#CCD-transactions)
     */
    private suspend fun handleCcdTx(
        data: Map<String, String>
    ) {
        Log.d("handling_ccd_tx")

        val amount = data["amount"]?.toBigInteger()
            ?: error("Amount is missing or invalid")

        val recipientAccountAddress = data["recipient"]
            ?: error("Recipient is missing or invalid")

        val recipientAccount = accountRepository.findByAddress(recipientAccountAddress)
            ?: error("Recipient account not found in the wallet: $recipientAccountAddress")

        transactionNotificationsManager.notifyCcdTransaction(
            receivedAmount = amount,
            account = recipientAccount,
            reference = data["reference"] ?: System.currentTimeMillis(),
        )
    }

    /**
     * @param data filled according to the
     * [specification](https://concordium.atlassian.net/wiki/spaces/EN/pages/1502019590/Message+data+payload#CIS-2-transactions)
     */
    private suspend fun handleCis2Tx(
        data: Map<String, String>
    ) {
        Log.d("handling_cis2_tx")

        val amount = data["amount"]?.toBigInteger()
            ?: error("Amount is missing or invalid")

        val recipientAccountAddress = data["recipient"]
            ?: error("Recipient is missing or invalid")

        val recipientAccount = accountRepository.findByAddress(recipientAccountAddress)
            ?: error("Recipient account not found in the wallet: $recipientAccountAddress")

        val contractIndex = data["contract_index"]
            ?: error("Contract index is missing")

        val tokenId = data["token_id"]
            ?: error("Token ID is missing")

        val existingContractToken = contractTokensRepository.find(
            accountAddress = recipientAccountAddress,
            contractIndex = contractIndex,
            tokenId = tokenId,
        )
        val token: Token

        if (existingContractToken == null) {
            Log.d(
                "adding_newly_received_token:" +
                        "\ncontractIndex=$contractIndex," +
                        "\ntokenId=$tokenId"
            )

            val isTokenUnique = data["token_is_unique"]
                ?.let { it == "true" }
                ?: error("Token unique flag is missing")

            val newlyReceivedContractToken =
                ContractToken(
                    id = 0,
                    isNewlyReceived = true,
                    tokenId = tokenId,
                    accountAddress = recipientAccountAddress,
                    isFungible = !isTokenUnique,
                    contractIndex = data["contract_index"]
                        ?: error("Contract index is missing"),
                    contractName = data["contract_name"]
                        ?: error("Contract name is missing"),
                    tokenMetadata = TokenMetadata(
                        decimals = data["token_decimals"]
                            ?.toIntOrNull()
                            ?: error("Token decimals is missing or invalid"),
                        name = data["token_name"]
                            ?: error("Token name is missing"),
                        symbol = data["token_symbol"]
                            ?: error("Token symbol is missing"),
                        unique = isTokenUnique,
                        thumbnail = data["token_thumbnail_url"]
                            ?.let(::UrlHolder),
                        display = data["token_display_url"]
                            ?.let(::UrlHolder),
                        description = null,
                    )
                )

            contractTokensRepository.insert(newlyReceivedContractToken)

            token = Token(newlyReceivedContractToken)
        } else {
            Log.d(
                "token_exists:" +
                        "\ncontractIndex=$contractIndex," +
                        "\ntokenId=$tokenId"
            )

            token = Token(existingContractToken)
        }

        transactionNotificationsManager.notifyCis2Transaction(
            receivedAmount = amount,
            token = token,
            account = recipientAccount,
            reference = data["reference"] ?: System.currentTimeMillis(),
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceCoroutineContext.cancel()
    }
}
