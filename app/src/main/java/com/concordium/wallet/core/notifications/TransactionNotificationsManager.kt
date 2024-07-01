package com.concordium.wallet.core.notifications

import android.app.NotificationManager
import com.concordium.wallet.data.preferences.NotificationsPreferences

class TransactionNotificationsManager(
    private val notificationManager: NotificationManager,
    private val notificationsPreferences: NotificationsPreferences,
) {
    // TODO: Create channels and standalone notifications for CCD and CIS2 transactions.
}
