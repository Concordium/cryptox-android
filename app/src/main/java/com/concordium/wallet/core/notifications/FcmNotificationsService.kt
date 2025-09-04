package com.concordium.wallet.core.notifications

import com.concordium.wallet.App
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.PLTRepository
import com.concordium.wallet.data.cryptolib.ContractAddress
import com.concordium.wallet.data.model.ContractToken
import com.concordium.wallet.data.model.ProtocolLevelToken
import com.concordium.wallet.data.model.toContractToken
import com.concordium.wallet.data.model.toProtocolLevelToken
import com.concordium.wallet.data.room.ContractTokenEntity
import com.concordium.wallet.data.room.ProtocolLevelTokenEntity
import com.concordium.wallet.ui.cis2.retrofit.MetadataApiInstance
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.toBigInteger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

class FcmNotificationsService : FirebaseMessagingService() {
    private val serviceCoroutineContext = Dispatchers.IO + SupervisorJob()

    override fun onNewToken(token: String) = runBlocking(serviceCoroutineContext) {
        Log.d("updating_subscriptions_with_new_token: \ntoken=$token")

        try {
            UpdateNotificationsSubscriptionUseCase().invoke()
            Log.d("trying update subscriptions with new token")
        } catch (error: Exception) {
            Log.e("failed_updating_subscriptions", error)
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
                    handleCcdTx(data = message.data)

                // CIS2 transactions.
                dataType == TransactionNotificationsManager.TYPE_CIS2_TX ->
                    handleCis2Tx(data = message.data)

                // PLT transactions.
                dataType == TransactionNotificationsManager.TYPE_PLT_TX ->
                    handlePltTx(data = message.data)

                // Announcements.
                notificationBody != null ->
                    handleAnnouncement(
                        notificationTitle = notificationTitle,
                        notificationBody = notificationBody,
                        messageId = message.messageId,
                    )

                else -> error("Unsupported message")
            }

            Log.d("message_handled: \nmessageId=${message.messageId}")
        } catch (error: Exception) {
            Log.e("failed_handling_message: \nmessageId=${message.messageId}", error)
        }
    }

    private fun handleAnnouncement(
        notificationTitle: String?,
        notificationBody: String,
        messageId: String?,
    ) {
        Log.d("handling_announcement: \nmessageId=$messageId")

        AnnouncementNotificationManager(application).notifyAnnouncement(
            title = notificationTitle,
            text = notificationBody,
            reference = messageId ?: System.currentTimeMillis(),
        )
    }

    /**
     * @param data filled according to the
     * [specification](https://concordium.atlassian.net/wiki/spaces/EN/pages/1502019590/Message+data+payload#CCD-transactions)
     */
    private suspend fun handleCcdTx(data: Map<String, String>) {
        Log.d("handling_ccd_tx")

        val amount = data.getAmount("amount")
        val recipientAccount = data.getRecipientAccount()

        TransactionNotificationsManager(application).notifyCcdTransaction(
            receivedAmount = amount,
            account = recipientAccount,
            reference = data["reference"] ?: System.currentTimeMillis(),
        )
    }

    /**
     * @param data filled according to the
     * [specification](https://concordium.atlassian.net/wiki/spaces/EN/pages/1502019590/Message+data+payload#CIS-2-transactions)
     */
    private suspend fun handleCis2Tx(data: Map<String, String>) {
        Log.d("handling_cis2_tx")

        val amount = data.getAmount("amount")
        val recipientAccount = data.getRecipientAccount()
        val contractAddress: ContractAddress = data["contract_address"]
            ?.let { App.appCore.gson.fromJson(it, ContractAddress::class.java) }
            ?: error("Contract address is missing")

        val tokenId = data.getByKey("token_id")
        val tokenMetadata: NotificationTokenMetadata = data["token_metadata"]
            ?.let { App.appCore.gson.fromJson(it, NotificationTokenMetadata::class.java) }
            ?: error("tokenMetadata is missing or invalid")
        Log.d("tokenMetadata: $tokenMetadata")

        val contractTokensRepository =
            ContractTokensRepository(App.appCore.session.walletStorage.database.contractTokenDao())

        val existingContractToken = contractTokensRepository.find(
            accountAddress = recipientAccount.address,
            contractIndex = contractAddress.index.toString(),
            token = tokenId,
        )
        val token: ContractToken = if (existingContractToken == null) {
            Log.d(
                "adding_newly_received_token:" +
                        "\ncontractAddress=$contractAddress," +
                        "\ntokenId=$tokenId"
            )

            val verifiedMetadata = MetadataApiInstance.safeMetadataCall(
                url = tokenMetadata.url,
                checksum = tokenMetadata.hash
            ).getOrThrow()

            Log.d("verifiedMetadata: $verifiedMetadata")

            val newlyReceivedContractToken =
                ContractTokenEntity(
                    id = 0,
                    isNewlyReceived = true,
                    token = tokenId,
                    accountAddress = recipientAccount.address,
                    contractIndex = contractAddress.index.toString(),
                    contractName = data["contract_name"]
                        ?: error("Contract name is missing"),
                    metadata = verifiedMetadata,
                    addedAt = System.currentTimeMillis()
                )

            contractTokensRepository.insert(newlyReceivedContractToken)

            newlyReceivedContractToken.toContractToken()
        } else {
            Log.d(
                "token_exists:" +
                        "\ncontractAddress=$contractAddress," +
                        "\ntokenId=$tokenId"
            )
            existingContractToken.toContractToken()
        }

        TransactionNotificationsManager(application).notifyCis2Transaction(
            receivedAmount = amount,
            token = token,
            account = recipientAccount,
            reference = data["reference"] ?: System.currentTimeMillis(),
        )
    }

    /**
     * @param data filled according to the
     * [specification](https://concordium.atlassian.net/wiki/spaces/PTS/pages/1502019590/Message+data+payload#PLT-transactions)
     */
    private suspend fun handlePltTx(data: Map<String, String>) {
        Log.d("handling_plt_tx")

        val amount = data.getAmount("value")
        val decimals = data.getByKey("decimals").toInt()
        val tokenId = data.getByKey("token_id")
        val recipientAccount = data.getRecipientAccount()

        val pltRepository =
            PLTRepository(App.appCore.session.walletStorage.database.protocolLevelTokenDao())

        val existingProtocolLevelToken = pltRepository.find(
            accountAddress = recipientAccount.address,
            tokenId = tokenId
        )

        val token: ProtocolLevelToken = if (existingProtocolLevelToken == null) {
            Log.d(
                "adding_newly_received_token:" +
                        "\naccountAddress=${recipientAccount.address}" +
                        "\ntokenId=$tokenId"
            )

            val newlyReceivedToken = ProtocolLevelTokenEntity(
                name = null,
                decimals = decimals,
                tokenId = tokenId,
                accountAddress = recipientAccount.address,
                metadata = null,
                isNewlyReceived = true
            )
            pltRepository.insert(newlyReceivedToken)
            newlyReceivedToken.toProtocolLevelToken()
        } else {
            Log.d("token_exists: \ntokenId=$tokenId")
            existingProtocolLevelToken.toProtocolLevelToken()
        }

        TransactionNotificationsManager(application).notifyPltTransaction(
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

    private data class NotificationTokenMetadata(
        @SerializedName("url")
        val url: String,
        @SerializedName("hash")
        val hash: String?,
    )

    // Helpers
    private fun Map<String, String>.getAmount(key: String) =
        this[key]?.toBigInteger() ?: error("$key is missing or invalid")

    private fun Map<String, String>.getByKey(key: String): String =
        this[key] ?: error("$key is missing or invalid")

    private suspend fun Map<String, String>.getRecipientAccount() =
        getByKey("recipient").let { address ->
            AccountRepository(App.appCore.session.walletStorage.database.accountDao())
                .findByAddress(address)
                ?: error("Recipient account not found in wallet: $address")
        }
}
