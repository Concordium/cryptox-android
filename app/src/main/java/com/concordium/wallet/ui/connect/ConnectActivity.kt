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
import com.bumptech.glide.Glide
import com.concordium.wallet.AppConfig
import com.concordium.wallet.Constants
import com.concordium.wallet.Constants.Extras.EXTRA_ADD_CONTACT
import com.concordium.wallet.Constants.Extras.EXTRA_CONNECT_URL
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.OfflineMockInterceptor
import com.concordium.wallet.data.backend.ws.WsTransport
import com.concordium.wallet.data.model.WsConnectionInfo
import com.concordium.wallet.data.model.WsMessageResponse
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.ui.connect.add_wallet.AddWalletNftActivity
import com.concordium.wallet.ui.connect.uni_ref.UniRefActivity
import com.concordium.wallet.util.Log
import com.google.gson.Gson
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

        findViewById<Button>(R.id.btnConnect).setOnClickListener(this::sendAccept)
        findViewById<Button>(R.id.btnCancel).setOnClickListener(this::sendCancel)

        val walletConnectPrefixes = setOf(
            DEFAULT_WALLET_CONNECT_PREFIX,
            "${getString(R.string.wc_scheme)}:",
        )
        val demoPayAndVerifyPrefix = getString(R.string.scheme) + "://demo_pay_and_verify"
        val supportedPrefixes =
            demoPayAndVerifyPrefix +
                    walletConnectPrefixes +
                    MARKETPLACE_CONNECT_PREFIX

        var isDepLink = false
        val connectUrl: String? =
            intent?.getStringExtra(EXTRA_CONNECT_URL).let { intentConnectUrl ->
                if (!intentConnectUrl.isNullOrEmpty()) {
                    intentConnectUrl
                } else {
                    isDepLink = true
                    val urlData = intent?.data
                    if (urlData != null
                        && urlData.isHierarchical
                        && urlData.getQueryParameter("uri") != null
                    )
                    // Case for NFT marketplace.
                        urlData.getQueryParameter("uri")
                    else
                    // Case for WalletConnect or something else.
                        urlData?.toString()
                }
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
            connectUrl.startsWith(MARKETPLACE_CONNECT_PREFIX) ->
                getBridgeInfo(connectUrl)

            connectUrl.startsWith(demoPayAndVerifyPrefix) ->
                demoPayAndVerify(connectUrl)

            walletConnectPrefixes.any { connectUrl.startsWith(it) } ->
                connectWc(connectUrl)
        }

        closeBtn?.visibility = View.VISIBLE
        closeBtn?.setOnClickListener {
            finish()
        }
    }

    private fun initializeOkkHttp(): OkHttpClient {
        var okHttpClientBuilder = OkHttpClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cache(null)
//            .addInterceptor(ModifyHeaderInterceptor())

        if (AppConfig.useOfflineMock) {
            okHttpClientBuilder = okHttpClientBuilder.addInterceptor(OfflineMockInterceptor())
        }
        return okHttpClientBuilder.build()
    }

    private fun showShopIcon(url: String) {
        Glide.with(applicationContext).load(url).placeholder(R.drawable.ic_favicon)
            .error(R.drawable.ic_favicon).into(shopLogo)
    }

//    https://cwb.spaceseven.cloud/condition/381743969094074370/!fI0wgh6arvEnpC8WWKIIYtB5G5fm@$nsd2JtfZydNgGxxK4HrROgUfjwt3y!Tw3qDVVkX1oGCegsxhk08A3yLVmGXz1YL73FWz3!QVb0Ep36mftbt7@o8Fy3QmHBNbf
//    https://cwb.spaceseven.cloud/condition/381743011635134466/SlOJhHSPcmuCW1op3l9K@L@F!n5c56rhwCmjpZ1aV4KpuC@nZsko@jNkyEhR2ucsV2whe2MHebD1Q!zGmTUX8sltQVUUxZNAYBpQQ6Uqxd66gU$nIv7TWOnBOd!an9VS

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
                    shopName.text = if (json.site.title.isNullOrEmpty()) {
                        "Spaceseven.com"
                    } else {
                        json.site.title
                    }
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

    private fun sendCancel(v: View) {
        wsTransport?.sendConnectionReject()
        finish()
    }

    private fun sendAccept(v: View) = wsTransport?.sendConnectionAccept()

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

    private fun demoPayAndVerify(connectUrl: String){
        val invoiceUrl = Uri.parse(connectUrl).getQueryParameter("invoice")
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_DEMO_PAY_AND_VERIFY_INVOICE_URL, invoiceUrl)
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
