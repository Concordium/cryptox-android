package com.concordium.wallet

object Constants {

    const val PAGINATION_COUNT = 26

    object QRPrefix {
        const val AIR_DROP = "airdrop://"
    }

    object Extras {
        const val EXTRA_SCANNED_QR_CONTENT = "scanned_qr_content"
        const val EXTRA_QR_CONNECT = "qr_connect"
        const val EXTRA_CONNECT_URL = "connect_url"
        const val EXTRA_AIR_DROP_PAYLOAD = "air_drop_url"
        const val EXTRA_ADD_CONTACT = "add_contact"
        const val EXTRA_SITE_INFO = "extra_site_info"
        const val EXTRA_MESSAGE_TYPE = "extra_message_type"
        const val EXTRA_TRANSACTION_INFO = "extra_transaction_info"
        const val EXTRA_WS_URL = "extra_ws_url"
        const val EXTRA_PROVIDER_DATA = "extra_provider_data"
        const val EXTRA_WALLET_DATA = "extra_wallet_data"
    }

    object Menu {
        const val SHOW = 1
        const val HIDE = 2
    }
}
