package com.concordium.wallet.util

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

object IntentUtil {

    fun openUrl(context: Context, url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
        ContextCompat.startActivity(context, browserIntent, null)
    }
}