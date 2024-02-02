package com.concordium.wallet.ui.airdrop

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Base64
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.bold
import androidx.core.text.underline
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.concordium.wallet.Constants
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil.formatGTU
import com.concordium.wallet.databinding.ActivityAirdropBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import timber.log.Timber

// {
//    "api_url": "https://stage.spaceseven.cloud/api/v2/airdrop/register-wallet",
//    "airdrop_id": "3446112874593",
//    "airdrop_name": "MasterChef",
//    "marketplace_name": "Spaceseven",
//    "marketplace_url": "https://stage.spaceseven.cloud",
//    "marketplace_icon": " https://weeny.link/1KRq431"
// }

data class AirDropPayload(
    @SerializedName("api_url")
    val apiUrl: String,
    @SerializedName("airdrop_id")
    val airdropId: Long,
    @SerializedName("airdrop_name")
    val airdropName: String,
    @SerializedName("marketplace_name")
    val marketplaceName: String,
    @SerializedName("marketplace_url")
    val marketplaceUrl: String,
    @SerializedName("marketplace_icon")
    val marketplaceIcon: String
) {
    override fun toString(): String {
        return "AirDropPayload(apiUrl='$apiUrl', airdropId='$airdropId', airdropName='$airdropName', marketplaceName='$marketplaceName', marketplaceUrl='$marketplaceUrl', marketplaceIcon='$marketplaceIcon')"
    }
}

class AirdropActivity : BaseActivity() {

    private lateinit var viewModel: AirDropViewModel
    private var binding: ActivityAirdropBinding? = null
    private var selectedAccount: Account? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAirdropBinding.inflate(layoutInflater)

        setContentView(binding?.root)

        val airDropPayload = intent?.getStringExtra(Constants.Extras.EXTRA_AIR_DROP_PAYLOAD)
        if (airDropPayload == null) {
            finish()
            return
        }

        println(">>>>>>>>>>>>>>>>>>> ")

        val payloadJson = try {
            String(Base64.decode(airDropPayload.replace("airdrop://", ""), Base64.DEFAULT)).trim()
        } catch (ex: Exception) {
            showErrorMessage("Malformed airdrop data")
            finish()
            return
        }

        val payload = try {
            Gson().fromJson(payloadJson, AirDropPayload::class.java)
        } catch (ex: Exception) {
            Timber.e("Unable parse jsom from QR. Exception: ${ex.message}")
            finish()
            return
        }

        println(payload)

        initUi(payload)
        initViewModel(payload)
    }

    private fun initUi(payload: AirDropPayload) {
        binding?.shopName?.text = payload.marketplaceName
        binding?.shopDesc?.text = payload.airdropName
        binding?.btnCancel?.setOnClickListener {
            finish()
        }
        binding?.toolbarLayout?.toolbarTitle?.text = "AirDrop"
        binding?.toolbarLayout?.toolbarCloseBtn?.visibility = View.VISIBLE
        binding?.toolbarLayout?.toolbarCloseBtn?.setOnClickListener {
            finish()
        }
        binding?.btnConnect?.setOnClickListener {
            viewModel.processAction(AirDropAction.GetWallets)
        }
        binding?.btnContinue?.setOnClickListener {
            finish()
        }
//        binding?.descBody?.text = getString(R.string.airdrop_connect_description, payload.marketplaceName, payload.airdropName)
        binding?.descBody?.text = getSpannableDescription(payload.marketplaceName, payload.airdropName, payload.marketplaceUrl)
        binding?.descBody?.movementMethod = LinkMovementMethod.getInstance()

        binding?.shopLogo?.let {
            Glide.with(applicationContext).load(payload.marketplaceIcon).into(it)
        }
    }

    private fun getSpannableDescription(marketplaceName: String, airdropName: String, marketplaceUrl: String): Spannable {
//        val txt = "Please confirm registration to AirDrop campaign at $marketplaceName $airdropName"
        val sb = SpannableStringBuilder()
            .append("Please confirm registration to AirDrop campaign at ")
            .bold {
                underline {
                    append(marketplaceName)
                }
            }
            .append(" $airdropName")

        val clickable = object : ClickableSpan() {
            override fun onClick(p0: View) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(marketplaceUrl)))
            }
        }

        sb.setSpan(clickable, 51, 51 + marketplaceName.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        return sb
    }

    private fun initViewModel(payload: AirDropPayload) {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AirDropViewModel::class.java]

        viewModel.processAction(AirDropAction.Init(payload.airdropId.toLong(), payload.apiUrl))
        viewModel.viewState.collectWhenStarted(this, ::processViewState)

        viewModel.onWalletReady().observe(this) { event ->
            event.contentOrNullIfUsed?.let {
                showWallets(it)
            }
        }

        viewModel.onError().observe(this) { event ->
            event.contentOrNullIfUsed?.let {
                Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
//                showErrorMessage(it)
                finish()
            }
        }

        viewModel.doRegistrationResponse().observe(this) { event ->
            event.contentOrNullIfUsed?.let {
                binding?.btnContinue?.visibility = View.VISIBLE
                binding?.btnConnect?.visibility = View.GONE
                binding?.btnCancel?.visibility = View.GONE
                binding?.descTitle?.visibility = View.GONE
                binding?.descBody?.visibility = View.GONE
                binding?.accountsPool?.removeAllViews()
                binding?.accountsPool?.visibility = View.GONE
                binding?.regResult?.visibility = View.VISIBLE
                binding?.regResult?.text = it.message
            }
        }
    }

    private fun processViewState(state: AirDropState) {
        when (state) {
            AirDropState.ConnectConfirm -> {}
            AirDropState.SelectWallet -> {
                binding?.descTitle?.text = "Select wallet"
                binding?.descBody?.visibility = View.GONE
                binding?.btnConnect?.text = "Select"
                binding?.btnConnect?.setOnClickListener {
                    if (selectedAccount != null) {
                        viewModel.processAction(AirDropAction.SendRegistration(selectedAccount!!))
                    } else {
                        showErrorMessage("No wallet selected")
                    }
                }
            }
            AirDropState.ShowResult -> {}
        }
    }

    private fun showErrorMessage(string: String) {
        binding?.toastLayoutTopError?.let {
            popup.showSnackbarError(it, string)
        }
    }

    private fun showWallets(wallets: List<Account>) {
        binding?.accountsPool?.removeAllViews()
        wallets.forEach { acc ->
            val v = layoutInflater.inflate(R.layout.account_info_row, null)
            v.findViewById<TextView>(R.id.accAddress).text = getString(R.string.acc_address_placeholder, acc.name, acc.address)
            val balance = formatGTU(acc.totalUnshieldedBalance)
            v.findViewById<TextView>(R.id.accBalance).text = getString(R.string.acc_balance_placeholder, balance)
            binding?.accountsPool?.addView(v)
            v.setOnClickListener {
                clearAccSelection()
                selectedAccount = acc
                v.findViewById<ConstraintLayout>(R.id.accRoot).background = resources.getDrawable(R.drawable.btn_round_outline_bg_active)
            }
        }
    }

    private fun clearAccSelection() {
        binding?.accountsPool?.children?.forEach {
            it.findViewById<ConstraintLayout>(R.id.accRoot).background = resources.getDrawable(R.drawable.btn_round_outline_bg)
        }
    }
}