package com.concordium.wallet.ui.connect.uni_ref

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.concordium.wallet.Constants
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.data.model.WsConnectionInfo
import com.concordium.wallet.data.model.WsMessageResponse
import com.concordium.wallet.data.util.CurrencyUtil.formatGTU
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.connect.ITransactionResult
import com.concordium.wallet.ui.connect.NotificationBottomSheet
import com.concordium.wallet.ui.connect.TransactionResult
import com.concordium.wallet.ui.connect.TransactionResultBottomSheet
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.math.BigInteger

class UniRefActivity : BaseActivity(
    R.layout.activity_create_nft,
    R.string.title_sell_nft
), ITransactionResult, AuthDelegate by AuthDelegateImpl() {

    private var transactionResultBottomSheet: TransactionResultBottomSheet? = null
    private var notificationBottomSheet: NotificationBottomSheet? = null
    private lateinit var siteInfo: WsConnectionInfo.SiteInfo
    private lateinit var payload: WsMessageResponse.Payload
    private lateinit var messageType: String
    private lateinit var viewModel: UniRefViewModel

    private val netCommissionMax by lazy {
        findViewById<TextView>(R.id.net_commission_value_max)
    }

    private val netCommissionMin by lazy {
        findViewById<TextView>(R.id.net_commission_value_min)
    }

    private val totalAmount by lazy {
        findViewById<TextView>(R.id.total_amount_value)
    }

    private val totalAmountHeader by lazy {
        findViewById<TextView>(R.id.total_amount_value_header)
    }

    private val descTitle by lazy {
        findViewById<TextView>(R.id.descTitle)
    }

    private val walletName by lazy {
        findViewById<TextView>(R.id.accAddress)
    }

    private val walletBalance by lazy {
        findViewById<TextView>(R.id.accBalance)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = intent?.extras
        val siteInfoJson: String? = bundle?.getString(Constants.Extras.EXTRA_SITE_INFO)
        val wsMessageJson: String? = bundle?.getString(Constants.Extras.EXTRA_TRANSACTION_INFO)
        val wsMessageType: String? = bundle?.getString(Constants.Extras.EXTRA_MESSAGE_TYPE)
        if (siteInfoJson == null || wsMessageJson == null || wsMessageType == null) {
            Toast.makeText(applicationContext, "Something going wrong", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val gson = Gson()
        siteInfo = gson.fromJson(siteInfoJson, WsConnectionInfo.SiteInfo::class.java)
        payload = gson.fromJson(wsMessageJson, WsMessageResponse.Payload::class.java)
        messageType = wsMessageType
        initializeViewModel()
        viewModel.initialize(payload, messageType)

        closeBtn?.visibility = View.VISIBLE
        closeBtn?.setOnClickListener {
            viewModel.reject()
            finish()
        }

        val title = viewModel.getTransactionTitle()
        descTitle.text = title
        setActionBarTitle(title)

        transactionResultBottomSheet = TransactionResultBottomSheet(this, this)
        notificationBottomSheet = NotificationBottomSheet(this, this)
    }

    override fun onResume() {
        super.onResume()

        initViews()
    }

    private fun initViews() {

        val shopName = findViewById<TextView>(R.id.shopName)
        val shopDescription = findViewById<TextView>(R.id.shopDesc)
        val shopLogo = findViewById<ImageView>(R.id.shopLogo)
        val amountTv = findViewById<TextView>(R.id.amount_value)

        shopName.text = siteInfo.title
        shopDescription.text = siteInfo.description
        Glide.with(applicationContext).load(siteInfo.iconLink).placeholder(R.drawable.ic_favicon)
            .error(R.drawable.ic_favicon).into(shopLogo)
        val amount = viewModel.getAmount().let(::formatGTU)
        amountTv.text = getString(R.string.amount, amount)
        netCommissionMax.text = getString(R.string.amount_max, "0")
        netCommissionMin.text = getString(R.string.amount, "0")
        totalAmount.text = getString(R.string.amount, amount)
        totalAmountHeader.text = getString(R.string.amount, amount)

        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            viewModel.reject()
            finish()
        }

        findViewById<Button>(R.id.btnApprove).setOnClickListener {
            viewModel.sendFunds()
        }
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[UniRefViewModel::class.java]

        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                super.onUnhandledEvent(value)
                when (value) {
                    12 -> {
                        Toast.makeText(
                            applicationContext,
                            "No account found for this wallet",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }

                    13 -> {
                        notificationBottomSheet?.setData(description = "Insufficient funds. You must have minimum 5.5 CCD to proceed.")
                    }
                }
            }
        })

        viewModel.insufficientFundsLiveData.observe(this, object : EventObserver<BigInteger>() {
            override fun onUnhandledEvent(value: BigInteger) {
                super.onUnhandledEvent(value)
                showError("Insufficient funds. You must have minimum ${formatGTU(value)} CCD to proceed.")
            }

//            notificationBottomSheet?.setData(description = "Insufficient funds. You must have minimum ${it.formatCCDCeil()} CCD to proceed.")
        })

        viewModel.backendErrorLiveData.observe(this, object : EventObserver<BackendError>() {
            override fun onUnhandledEvent(value: BackendError) {
                val msg = if (value.errorMessage.isNullOrEmpty()) {
                    "Unknown backend error"
                } else {
                    value.errorMessage ?: "Unknown backend error"
                }

                notificationBottomSheet?.setData(
                    description = msg,
                    type = NotificationBottomSheet.Type.ERROR
                )
            }
        })

        viewModel.transactionFeeLiveData.observe(this) {
            val am = payload.amount
            val maxFee = formatGTU(it)
            val minFee = formatGTU(it / 3.toBigInteger())
            val total = formatGTU(am + it)
            netCommissionMax.text = getString(R.string.amount_max, maxFee)
            netCommissionMin.text = getString(R.string.amount, minFee)
            totalAmount.text = getString(R.string.amount, total)
            totalAmountHeader.text = getString(R.string.amount, total)
        }

        viewModel.transactionResultData.observe(this) {
            showTransactionResultBottomSheet(it)
        }

        viewModel.showAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showAuthentication(
                        activity = this@UniRefActivity,
                        onAuthenticated = viewModel::continueWithPassword
                    )
                }
            }
        })

        viewModel.gotoSendFundsConfirmLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
//                    finish()
                }
            }
        })

        viewModel.walletDataLiveData.observe(this, object : EventObserver<WalletData>() {
            override fun onUnhandledEvent(value: WalletData) {
                walletName.text = "${value.name} (${value.address})"
                walletBalance.text = getString(R.string.amount, formatGTU(value.balance))
            }
        })
    }

    private fun showTransactionResultBottomSheet(transfer: TransactionResult) =
        GlobalScope.launch(Dispatchers.Main) {
            transactionResultBottomSheet?.setData(transfer)
        }

    private fun createConfirmString(): String? {
        val amount = viewModel.getAmount()
        val cost = viewModel.transactionFeeLiveData.value
        val recipient = viewModel.selectedRecipient
        if (cost == null || recipient == null) {
            // TODO Show error to user
            return null
        }
        val amountString = formatGTU(amount, withGStroke = true)
        val costString = formatGTU(cost, withGStroke = true)

        return if (viewModel.isShielded) {
            if (viewModel.isTransferToSameAccount()) {
                getString(
                    R.string.send_funds_confirmation_unshield,
                    amountString,
                    recipient.name,
                    costString,
                    ""
                )
            } else {
                getString(
                    R.string.send_funds_confirmation_send_shielded,
                    amountString,
                    recipient.name,
                    costString,
                    ""
                )
            }
        } else {
            if (viewModel.isTransferToSameAccount()) {
                getString(
                    R.string.send_funds_confirmation_shield,
                    amountString,
                    recipient.name,
                    costString,
                    ""
                )
            } else {
                getString(
                    R.string.send_funds_confirmation,
                    amountString,
                    recipient.name,
                    costString,
                    ""
                )
            }
        }
    }

    override fun onResultBottomSheetDismissed() {
        transactionResultBottomSheet?.dismiss()
        notificationBottomSheet?.dismiss()
        finish()
    }
}
