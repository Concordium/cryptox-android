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
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import java.net.HttpURLConnection

class UpdateNotificationsSubscriptionUseCase(
    private val accountRepository: AccountRepository,
    private val notificationsPreferences: NotificationsPreferences,
    private val notificationsBackend: NotificationsBackend,
    private val application: Application,
) {
    constructor(application: Application) : this(
        accountRepository = WalletDatabase.getDatabase(application).accountDao()
            .let(::AccountRepository),
        notificationsPreferences = NotificationsPreferences(application),
        notificationsBackend = App.appCore.getNotificationsBackend(),
        application = application
    )

    /**
     * @return **true** on successful update
     */
    suspend operator fun invoke(
        isCcdTxEnabled: Boolean = notificationsPreferences.areCcdTxNotificationsEnabled,
        isCis2TxEnabled: Boolean = notificationsPreferences.areCis2TxNotificationsEnabled,
    ): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val fcmToken = when (googleApiAvailability.isGooglePlayServicesAvailable(application)) {
            ConnectionResult.SUCCESS -> {
                try {
                    FirebaseMessaging.getInstance().token.await()
                } catch (e: Exception) {
                    Log.e("token_not_available", e)
                    return false
                }
            }

            else -> {
                Log.e("google_api_not_available")
                return false
            }
        }

        val accounts = accountRepository.getAllDone()

        val topics: Set<NotificationsTopic> = buildSet {
            if (isCcdTxEnabled) {
                add(NotificationsTopic.CCD_TRANSACTIONS)
            }
            if (isCis2TxEnabled) {
                add(NotificationsTopic.CIS2_TRANSACTIONS)
            }
        }

        if (topics.isNotEmpty()) {
            Log.d(
                "updating_subscription:" +
                        "\ntoken=$fcmToken," +
                        "\ntopics=$topics," +
                        "\naccounts=${accounts.size}"
            )

            val request = UpdateSubscriptionRequest(
                preferences = topics,
                accounts = accounts.map(Account::address).toSet(),
                fcmToken = fcmToken,
            )

            return try {
                Log.d("attempt_to_update_subscription")
                notificationsBackend.updateSubscription(request).isSuccess
            } catch (e: Exception) {
                Log.e("failed_to_update_subscription", e)
                false
            }
        } else {
            Log.d(
                "clearing_subscription:" +
                        "\ntoken=$fcmToken"
            )

            val request = UpdateSubscriptionRequest(
                fcmToken = fcmToken
            )

            return try {
                Log.d("attempt_to_unsubscribe")
                notificationsBackend.unsubscribe(request).isSuccess
            } catch (e: Exception) {
                if (e is HttpException && e.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                    // Not found is expected when already unsubscribed.
                    Log.d("already_unsubscribed")
                    true
                } else {
                    Log.e("failed_to_unsubscribe", e)
                    false
                }
            }
        }
    }
}
