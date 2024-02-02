package com.concordium.wallet.ui.connect.add_wallet

import android.os.Bundle
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import com.bumptech.glide.Glide
import com.concordium.wallet.Constants
import com.concordium.wallet.R
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.backend.ws.WsTransport
import com.concordium.wallet.data.model.WsConnectionInfo
import com.concordium.wallet.data.model.WsMessageResponse
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.data.model.AccountInfo
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.util.CurrencyUtil
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AddWalletNftActivity : BaseActivity(R.layout.activity_connect, R.string.title_add_wallet) {

    private var accountRepository: AccountRepository? = null
    private var siteInfo: WsConnectionInfo.SiteInfo? = null
    private var payload: WsMessageResponse.Payload? = null

    private var accountsPool: LinearLayout? = null
    private var selectedAccount: Account? = null

    private val descTitle by lazy {
        findViewById<TextView>(R.id.descTitle)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = intent?.extras
        val siteInfoJson: String? = bundle?.getString(Constants.Extras.EXTRA_SITE_INFO)
        val gson = Gson()
        siteInfo = gson.fromJson(siteInfoJson, WsConnectionInfo.SiteInfo::class.java)

        descTitle.setText(R.string.title_add_wallet)
        accountsPool = findViewById(R.id.accountsPool)

        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        accountRepository = AccountRepository(accountDao)
    }

    override fun onResume() {
        super.onResume()

        initViews()

        getWallets()
    }

    private fun initViews() {

        val shopName = findViewById<TextView>(R.id.shopName)
        val shopDescription = findViewById<TextView>(R.id.shopDesc)
        val shopLogo = findViewById<ImageView>(R.id.shopLogo)

        shopName.text = siteInfo?.title
        shopDescription.text = siteInfo?.description
        Glide.with(applicationContext).load(siteInfo?.iconLink).placeholder(R.drawable.ic_favicon).error(R.drawable.ic_favicon).into(shopLogo)

        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            WsTransport.sendTransactionResult()
            finish()
        }

        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            sendWallet()
        }
    }

    private fun sendWallet() {
        if (selectedAccount != null) {
            val toSend = mutableListOf<AccountInfo.WalletInfo>()
            val wallet = AccountInfo.WalletInfo(
                selectedAccount!!.address,
                "${selectedAccount!!.totalUnshieldedBalance}"
            )
            toSend.add(wallet)
            finish()
            val request = AccountInfo(data = toSend)
            WsTransport.sendAccountInfo(request)
        } else {
            Toast.makeText(applicationContext, "No wallets selected", Toast.LENGTH_LONG).show()
        }
    }

    private fun getWallets() = GlobalScope.launch(Dispatchers.IO) {
        val wallets = accountRepository?.getAllDone()
            ?.filterNot(Account::readOnly)
            ?: emptyList()
        runOnUiThread {
            accountsPool?.removeAllViews()
            wallets.forEach { acc ->
                val v = layoutInflater.inflate(R.layout.account_info_row, null)
                val name = acc.name
                v.findViewById<TextView>(R.id.accAddress).text =
                    if (name.isNotEmpty())
                        getString(R.string.acc_address_placeholder, name, acc.address)
                    else
                        acc.address
                val atDisposalBalance =
                    acc.getAtDisposalWithoutStakedOrScheduled(acc.totalUnshieldedBalance)
                v.findViewById<TextView>(R.id.accBalance).text = getString(
                    R.string.acc_balance_placeholder,
                    CurrencyUtil.formatGTU(atDisposalBalance, true)
                )
                accountsPool?.addView(v)
                v.setOnClickListener {
                    clearAccSelection()
                    selectedAccount = acc
                    v.findViewById<ConstraintLayout>(R.id.accRoot).background = resources.getDrawable(R.drawable.btn_round_outline_bg_active)
                }
            }
        }
    }

    private fun clearAccSelection() {
        accountsPool?.children?.forEach {
            it.findViewById<ConstraintLayout>(R.id.accRoot).background = resources.getDrawable(R.drawable.btn_round_outline_bg)
        }
    }
}
