package com.concordium.wallet.core.notifications

import com.concordium.wallet.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmNotificationsService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Log.d(
            "token_generated:" +
                    "\ntoken=$token"
        )
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(
            "message_received:" +
                    "\nfrom=${message.from}" +
                    "\ntitle=${message.notification?.title}," +
                    "\ndata=${message.data}"
        )
    }
}
