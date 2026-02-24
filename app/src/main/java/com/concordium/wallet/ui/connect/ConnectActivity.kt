@file:OptIn(DelicateCoroutinesApi::class)

package com.concordium.wallet.ui.connect

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.net.toUri
import com.concordium.wallet.Constants.Extras.EXTRA_ADD_CONTACT
import com.concordium.wallet.Constants.Extras.EXTRA_CONNECT_URL
import com.concordium.wallet.R
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi

class ConnectActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val walletConnectPrefixes = setOf(
            DEFAULT_WALLET_CONNECT_PREFIX,
            "${getString(R.string.wc_scheme)}:",
        )
        val supportedPrefixes = walletConnectPrefixes + MARKETPLACE_CONNECT_PREFIX

        var isDepLink = false
        var isWalletConnect = false

        val intentUri =
            intent
                ?.getStringExtra(EXTRA_CONNECT_URL)
                ?.let {
                    isDepLink = true
                    it.toUri()
                }
                ?: intent?.data
        val intentUriString = intentUri?.toString()

        val connectUrl =
            if (intentUriString != null &&
                walletConnectPrefixes.any { intentUriString.startsWith(it) }
            ) {
                isWalletConnect = true
                intentUriString
            } else if (intentUri != null && intentUri.isHierarchical) {
                // Case for NFT marketplace.
                intentUri.getQueryParameter("uri")
            } else {
                // Case for something else.
                intentUriString
            }

        val isAddContact = intent?.getBooleanExtra(EXTRA_ADD_CONTACT, false) ?: false

        if (connectUrl.isNullOrEmpty()) {
            finish()
            Log.i(">>>>>>>>>>>>>>>>>>>>>>> Wrong connect URL finishing")
            return
        }

        Log.i(">>>>>>>>>>>>>>>>>>>>>>> connect_url 2: $connectUrl")

        if (connectUrl.isEmpty() || supportedPrefixes.none { connectUrl.startsWith(it) }) {
            if (isAddContact) {
                finish()
                return
            }
            Toast.makeText(
                applicationContext,
                "The ${
                    if (isDepLink) {
                        "link"
                    } else {
                        "QR"
                    }
                } contains wrong data. Reload the page and try again",
                Toast.LENGTH_LONG
            ).show()
            finish()
            Log.i(">>>>>>>>>>>>>>>>>>>>>>> Wrong connect URL finishing 2")
            return
        }

        when {
            isWalletConnect -> connectWc(connectUrl)

        }
    }

    private fun connectWc(wcUri: String) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_WALLET_CONNECT_URI, wcUri)
        )
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        return false
    }

    private companion object {
        private const val MARKETPLACE_CONNECT_PREFIX = "https://"
        private const val DEFAULT_WALLET_CONNECT_PREFIX = "wc:"
    }
}
