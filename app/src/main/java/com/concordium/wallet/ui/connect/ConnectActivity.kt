@file:OptIn(DelicateCoroutinesApi::class)

package com.concordium.wallet.ui.connect

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.concordium.wallet.Constants
import com.concordium.wallet.Constants.Extras.EXTRA_ADD_CONTACT
import com.concordium.wallet.Constants.Extras.EXTRA_CONNECT_URL
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.ws.WsTransport
import com.concordium.wallet.data.model.WsConnectionInfo
import com.concordium.wallet.data.model.WsMessageResponse
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.ui.connect.add_wallet.AddWalletNftActivity
import com.concordium.wallet.ui.connect.uni_ref.UniRefActivity
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel
import com.concordium.wallet.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ConnectActivity : BaseActivity(R.layout.activity_connect) {

    private lateinit var shopName: TextView
    private lateinit var shopDescription: TextView
    private lateinit var shopLogo: ImageView
    private var wsTransport: WsTransport? = null
    private var currentSiteInfo: WsConnectionInfo.SiteInfo? = null
    private var accountsPool: LinearLayout? = null

    private val gson by lazy {
        Gson()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        shopName = findViewById(R.id.shopName)
        shopDescription = findViewById(R.id.shopDesc)
        shopLogo = findViewById(R.id.shopLogo)
        accountsPool = findViewById(R.id.accountsPool)

        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            sendAccept()
        }
        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            sendCancel()
        }

        val walletConnectPrefixes = setOf(
            WalletConnectViewModel.WC_URI_PREFIX,
            WalletConnectViewModel.SPECIFIC_WC_URI_PREFIX
        )
        val supportedPrefixes = walletConnectPrefixes + MARKETPLACE_CONNECT_PREFIX

        var isDepLink = false
        var isWalletConnect = false

        val intentUri =
            intent
                ?.getStringExtra(EXTRA_CONNECT_URL)
                ?.let {
                    isDepLink = true
                    Uri.parse(it)
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
            isWalletConnect ->
                connectWc(connectUrl)

            connectUrl.startsWith(MARKETPLACE_CONNECT_PREFIX) ->
                getBridgeInfo(connectUrl)
        }

        closeBtn?.visibility = View.VISIBLE
        closeBtn?.setOnClickListener {
            finish()
        }
    }

    private fun initializeOkkHttp(): OkHttpClient {
        return OkHttpClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cache(null)
            .build()
    }

    private fun getBridgeInfo(connectUrl: String) = GlobalScope.launch(Dispatchers.IO) {
        try {
            val body = "".toRequestBody("application/json".toMediaTypeOrNull())

            val req = Request.Builder()
                .url(connectUrl)
                .post(body)
                .build()

            val okHttpClient = initializeOkkHttp()
            val call = okHttpClient.newCall(req)

            val resp = call.execute()
            if (resp.code == 200) {
                val b = resp.body?.string()
                Log.i(">>>>>>>>>>>>>>>>>>>>>>> Body: $b")
                val json = gson.fromJson(b, WsConnectionInfo::class.java)
                println(">>>>>>>>>>>>>>>>>>>>>>> JSON: $json")
                connectWs(json.wsConnLink)
                println(">>>>>>>>>>>>>>>>>>>>>>> Mark 1")
                currentSiteInfo = json.site
                runOnUiThread {
                    shopName.text = json.site.title.ifEmpty { "Spaceseven.com" }
                    shopDescription.text = json.site.description
//                    showShopIcon(json.site.iconLink)
                }
            } else {
                Log.i(">>>>>>>>>>>>>>>>> ${resp.body?.string()}")
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "The data is expired or incorrect",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        } catch (ex: Exception) {
            runOnUiThread {
                Log.e(ex.message ?: "exception", ex)
                val err = BackendErrorHandler.getExceptionStringRes(ex)
                Log.i(">>>>>>>>>>>>>>>>> ${getString(err)}")
                Toast.makeText(
                    applicationContext,
                    "Invalid data received. Can't continue",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun sendCancel() {
        wsTransport?.sendConnectionReject()
        finish()
    }

    private fun sendAccept() = wsTransport?.sendConnectionAccept()

    private fun connectWs(wsUrl: String) {
        wsTransport = WsTransport.connect(wsUrl)
        wsTransport?.subscribe { msg, err ->
            if (err != null) {
                Toast.makeText(applicationContext, "Error: ${err.message}", Toast.LENGTH_SHORT)
                    .show()
                finish()
                return@subscribe
            }

            when (msg?.messageType) {
                WsMessageResponse.MESSAGE_TYPE_ACCOUNT_INFO -> {
                    Intent(applicationContext, AddWalletNftActivity::class.java).also {
                        val bundle = Bundle()
                        bundle.putString(
                            Constants.Extras.EXTRA_SITE_INFO,
                            currentSiteInfo?.toJson()
                        )
                        it.putExtras(bundle)
                        startActivity(it)
                        finish()
                    }
                }

                WsMessageResponse.MESSAGE_TYPE_SIMPLE_TRANSFER,
                WsMessageResponse.MESSAGE_TYPE_TRANSACTION,
                -> {
                    Intent(applicationContext, UniRefActivity::class.java).also {
                        val bundle = Bundle()
                        bundle.putString(
                            Constants.Extras.EXTRA_SITE_INFO,
                            currentSiteInfo?.toJson()
                        )
                        bundle.putString(
                            Constants.Extras.EXTRA_TRANSACTION_INFO,
                            msg.data?.toJson()
                        )
                        bundle.putString(
                            Constants.Extras.EXTRA_MESSAGE_TYPE,
                            msg.messageType
                        )
                        it.putExtras(bundle)
                        startActivity(it)
                        finish()
                    }
                }

                else -> {
                    Toast.makeText(
                        this,
                        R.string.wallet_connect_error_invalid_request,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
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

interface WsMessage
