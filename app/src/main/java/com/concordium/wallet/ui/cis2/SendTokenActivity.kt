package com.concordium.wallet.ui.cis2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivitySendTokenBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.SendTokenViewModel.Companion.SEND_TOKEN_DATA
import com.concordium.wallet.ui.recipient.recipientlist.RecipientListActivity
import com.concordium.wallet.ui.transaction.sendfunds.AddMemoActivity
import com.concordium.wallet.uicore.view.ThemedCircularProgressDrawable
import com.concordium.wallet.util.CBORUtil
import com.concordium.wallet.util.KeyboardUtil
import com.concordium.wallet.util.KeyboardUtil.showKeyboard
import com.concordium.wallet.util.getSerializable
import java.math.BigInteger

class SendTokenActivity : BaseActivity(R.layout.activity_send_token, R.string.cis_send_funds) {
    private lateinit var binding: ActivitySendTokenBinding
    private val viewModel: SendTokenViewModel by viewModels()

    companion object {
        const val ACCOUNT = "ACCOUNT"
        const val TOKEN = "TOKEN"
        const val PARENT_ACTIVITY = "PARENT_ACTIVITY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendTokenBinding.bind(findViewById(R.id.root_layout))
        viewModel.sendTokenData.account = intent.getSerializable(ACCOUNT, Account::class.java)
        viewModel.chooseToken.postValue(intent.getSerializable(TOKEN, Token::class.java))
        initObservers()
        initFragmentListener()
        initViews()
        hideActionBarBack(isVisible = true)
    }

    override fun onResume() {
        super.onResume()
        enableSend()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.dispose()
    }

    private fun initViews() {
        binding.amount.hint = BigInteger.ZERO.toString()
        binding.atDisposal.text = CurrencyUtil.formatGTU(
            viewModel.sendTokenData.account?.balanceAtDisposal ?: BigInteger.ZERO
        )
        initializeAmount()
        initializeMax()
        initializeAddressBook()
        initializeSend()
        initializeSearchToken()
        viewModel.getGlobalInfo()
        viewModel.getAccountBalance()
    }

    private fun initializeSend() {
        binding.continueBtn.setOnClickListener {
            send()
        }
    }

    private fun send() {
        binding.continueBtn.isEnabled = false
        gotoReceipt()
    }

    private fun initializeSearchToken() {
        binding.content.setOnClickListener {
            val intent = Intent(this, SelectTokenActivity::class.java).apply {
                putExtra(SelectTokenActivity.SELECT_TOKEN_ACCOUNT, viewModel.sendTokenData.account)
            }
            getResultToken.launch(intent)
        }
    }

    private fun initializeAmount() {
        binding.amount.addTextChangedListener {
            viewModel.sendTokenData.amount =
                CurrencyUtil.toGTUValue(it.toString(), viewModel.sendTokenData.token)
                    ?: BigInteger.ZERO
            if (it.toString().isEmpty()) {
                binding.balanceSymbol.alpha = 0.5f
            } else {
                binding.balanceSymbol.alpha = 1f
            }
            viewModel.loadEURRate()
            viewModel.loadTransactionFee()
            enableSend()
        }
        binding.amount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (viewModel.sendTokenData.amount.signum() == 0) {
                    binding.amount.setText("")
                }
                showKeyboard(this, binding.amount)
            }
        }
        binding.amount.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    KeyboardUtil.hideKeyboard(this)
                    if (enableSend())
                        send()
                    true
                }

                else -> false
            }
        }
        binding.balanceSymbol.setOnClickListener {
            showKeyboard(this, binding.amount)
        }
    }

    private fun initializeMax() {
        binding.sendAllButton.setOnClickListener {
            var decimals = 6
            viewModel.sendTokenData.token?.let { token ->
                if (!token.isCcd)
                    decimals = token.decimals
            }
            binding.amount.setText(
                CurrencyUtil.formatGTU(
                    viewModel.sendTokenData.max ?: BigInteger.ZERO, decimals
                )
            )
            enableSend()
        }
    }

    private fun enableSend(): Boolean {
        binding.continueBtn.isEnabled = viewModel.canSend
        return binding.continueBtn.isEnabled
    }

    private fun initializeAddressBook() {
        binding.recipientLayout.setOnClickListener {
            val intent = Intent(this, RecipientListActivity::class.java)
            intent.putExtra(RecipientListActivity.EXTRA_SHIELDED, viewModel.sendTokenData.account)
            intent.putExtra(RecipientListActivity.EXTRA_SENDER_ACCOUNT, viewModel.sendTokenData.account)
            getResultRecipient.launch(intent)
        }
    }

    private fun goToEnterMemo() {
        val intent = Intent(this, AddMemoActivity::class.java)
        intent.putExtra(AddMemoActivity.EXTRA_MEMO, viewModel.getMemoText())
        getResultMemo.launch(intent)
    }

    private val getResultMemo =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getStringExtra(AddMemoActivity.EXTRA_MEMO)?.let { memo ->
                    handleMemo(memo)
                }
            }
        }

    private val getResultRecipient =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getSerializable(
                    RecipientListActivity.EXTRA_RECIPIENT,
                    Recipient::class.java
                )?.let { recipient ->
                    viewModel.onReceiverEntered(recipient.address)
                    binding.recipientPlaceholder.visibility = View.GONE
                    binding.recipientNameLayout.visibility = View.VISIBLE
                    binding.recipientAddress.text = recipient.address

                    if (recipient.name.isNotEmpty()) {
                        onReceiverNameFound(recipient.name)
                    } else {
                        binding.recipientName.visibility = View.GONE
                    }
                }
            }
        }

    private val getResultToken =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getSerializable(
                    SelectTokenActivity.SELECT_TOKEN_ACCOUNT,
                    Token::class.java
                )?.let { token ->
                    viewModel.chooseToken.postValue(token)
                }
            }
        }

    private fun clearMemo() =
        handleMemo("")

    private fun handleMemo(memoText: String) {
        if (memoText.isNotEmpty()) {
            viewModel.setMemo(CBORUtil.encodeCBOR(memoText))
            setMemoText(memoText)
        } else {
            viewModel.setMemo(null)
            setMemoText("")
        }
    }

    private fun setMemoText(memoText: String) {
        if (memoText.isNotEmpty()) {
            binding.addMemo.text = memoText
        } else {
            binding.addMemo.text = getString(R.string.cis_add_memo)
        }
    }

    private fun onReceiverNameFound(name: String) {
        viewModel.onReceiverNameFound(name)
        binding.recipientName.visibility = View.VISIBLE
        binding.recipientName.text = name
    }

    private fun initObservers() {
        viewModel.waiting.observe(this) { waiting ->
            showWaiting(waiting)
        }
        viewModel.chooseToken.observe(this) { token ->
            setTokenIcon(token)

            val decimals = token.decimals
            binding.balance.text =
                CurrencyUtil.formatGTU(token.balance, decimals)
            binding.token.text =
                if (token.isUnique)
                    token.name
                else
                    token.symbol
            if (token.isUnique && token.balance.signum() > 0) {
                // For owned NFTs, prefill the amount (quantity) which is 1
                // for smoother experience.
                binding.amount.setText("1")
                binding.balanceSymbol.visibility = View.GONE
            } else {
                // Clearing the text reveals the "0,00" hint.
                binding.amount.text.clear()
                binding.balanceSymbol.visibility = View.VISIBLE
            }
            binding.amount.decimals = decimals
            // For non-CCD tokens Max is always available.
            binding.sendAllButton.isEnabled = !token.isCcd
            binding.balanceSymbol.text = token.symbol

            if (!token.isCcd) {
                binding.addMemo.visibility = View.GONE
                binding.atDisposalLayout.visibility = View.GONE
                binding.balance.visibility = View.VISIBLE
                binding.eurRate.visibility = View.GONE
            } else {
                binding.addMemo.visibility = View.VISIBLE
                binding.addMemo.setOnClickListener {
                    if (viewModel.showMemoWarning()) {
                        MemoNoticeDialog().showSingle(
                            supportFragmentManager,
                            MemoNoticeDialog.TAG
                        )
                    } else {
                        goToEnterMemo()
                    }
                }
                binding.atDisposalLayout.visibility = View.VISIBLE
                binding.balance.visibility = View.GONE
                binding.eurRate.visibility = View.VISIBLE
            }
            // This also initiates fee loading.
            clearMemo()
        }

        viewModel.feeReady.observe(this) { fee ->
            // Null value means the fee is outdated.
            binding.fee.text =
                if (fee != null)
                    getString(
                        R.string.cis_estimated_fee,
                        CurrencyUtil.formatGTU(fee)
                    )
                else
                    ""
            binding.fee.isVisible = fee != null
            binding.sendAllButton.isEnabled = true
            binding.amountError.isVisible =
                viewModel.sendTokenData.token != null
                        && !viewModel.hasEnoughFunds()

            enableSend()
        }
        viewModel.errorInt.observe(this) {
            Toast.makeText(this, getString(it), Toast.LENGTH_SHORT).show()
        }
        viewModel.eurRateReady.observe(this) { rate ->
            binding.eurRate.text =
                if (rate != null)
                    getString(
                        R.string.cis_estimated_eur_rate,
                        rate
                    )
                else
                    ""
        }
    }

    private fun setTokenIcon(token: Token) {
        val tokenMetadata = token.metadata
        if (tokenMetadata?.thumbnail != null && !tokenMetadata.thumbnail.url.isNullOrBlank()) {
            Glide.with(this)
                .load(tokenMetadata.thumbnail.url)
                .override(resources.getDimensionPixelSize(R.dimen.cis_token_icon_size))
                .placeholder(ThemedCircularProgressDrawable(this))
                .error(R.drawable.mw24_ic_token_placeholder)
                .fitCenter()
                .into(binding.tokenIcon)
        } else if (token.isCcd) {
            Glide.with(this)
                .load(R.drawable.mw24_ic_ccd)
                .into(binding.tokenIcon)
        } else {
            Glide.with(this)
                .load(R.drawable.mw24_ic_token_placeholder)
                .into(binding.tokenIcon)
        }
    }

    private fun initFragmentListener() {
        supportFragmentManager.setFragmentResultListener(
            MemoNoticeDialog.ACTION_REQUEST,
            this
        ) { _, bundle ->
            val showAgain = MemoNoticeDialog.getResult(bundle)
            if (!showAgain) {
                viewModel.dontShowMemoWarning()
            }
            goToEnterMemo()
        }
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressBar.visibility = if (waiting) View.VISIBLE else View.GONE
    }

    private fun gotoReceipt() {
        val intent = Intent(this, SendTokenReceiptActivity::class.java)
        intent.putExtra(SEND_TOKEN_DATA, viewModel.sendTokenData)
        intent.putExtra(
            SendTokenReceiptActivity.PARENT_ACTIVITY,
            checkNotNull(this.intent.getStringExtra(PARENT_ACTIVITY)) {
                "Missing $PARENT_ACTIVITY extra"
            }
        )
        startActivityForResultAndHistoryCheck(intent)
    }
}
