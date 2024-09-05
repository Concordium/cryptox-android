package com.concordium.wallet.core.notifications

import android.app.Application
import com.concordium.wallet.App
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.backend.notifications.NotificationsBackend
import com.concordium.wallet.data.backend.notifications.UpdateSubscriptionRequest
import com.concordium.wallet.data.model.NotificationsTopic
import com.concordium.wallet.data.preferences.NotificationsPreferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class UpdateNotificationsSubscriptionUseCase(
    private val accountRepository: AccountRepository,
    private val notificationsPreferences: NotificationsPreferences,
    private val notificationsBackend: NotificationsBackend,
) {
    constructor(application: Application) : this(
        accountRepository = WalletDatabase.getDatabase(application).accountDao()
            .let(::AccountRepository),
        notificationsPreferences = NotificationsPreferences(application),
        notificationsBackend = App.appCore.getNotificationsBackend(),
    )

    suspend operator fun invoke(
        isCcdTxEnabled: Boolean = notificationsPreferences.areCcdTxNotificationsEnabled,
        isCis2TxEnabled: Boolean = notificationsPreferences.areCis2TxNotificationsEnabled
    ): Boolean {
        val fcmToken = FirebaseMessaging.getInstance().token.await()
        val accounts = accountRepository.getAllDone()

        val topics: Set<NotificationsTopic> = buildSet {
            if (isCcdTxEnabled) {
                add(NotificationsTopic.CCD_TRANSACTIONS)
            }
            if (isCis2TxEnabled) {
                add(NotificationsTopic.CIS2_TRANSACTIONS)
            }
        }
        val request: UpdateSubscriptionRequest

        if (topics.isNotEmpty()) {
            Log.d(
                "updating_subscription:" +
                        "\ntoken=$fcmToken," +
                        "\ntopics=$topics," +
                        "\naccounts=${accounts.size}"
            )

            request = UpdateSubscriptionRequest(
                preferences = topics,
                accounts = accounts.map(Account::address).toSet(),
                fcmToken = fcmToken,
            )
        } else {
            Log.d(
                "clearing_subscription:" +
                        "\ntoken=$fcmToken"
            )

            request = UpdateSubscriptionRequest(
                preferences = emptySet(),
                accounts = emptySet(),
                fcmToken = fcmToken
            )
        }

        return try {
            Log.d("attempt_to_update_subscription")
            notificationsBackend.updateSubscription(request).isSuccess
        } catch (e: Exception) {
            Log.e("failed_to_update_subscription", e)
            false
        }
    }
}
