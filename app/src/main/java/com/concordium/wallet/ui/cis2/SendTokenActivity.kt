package com.concordium.wallet.ui.cis2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.NewContractToken
import com.concordium.wallet.data.model.NewToken
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
import com.concordium.wallet.util.KeyboardUtil
import com.concordium.wallet.util.KeyboardUtil.showKeyboard
import com.concordium.wallet.util.getSerializable
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.math.BigInteger

class SendTokenActivity : BaseActivity(R.layout.activity_send_token, R.string.cis_send_funds) {
    private lateinit var binding: ActivitySendTokenBinding
    private val viewModel: SendTokenViewModel by viewModel {
        parametersOf(
            SendTokenData(
                account = intent.getSerializable(ACCOUNT, Account::class.java),
                token = intent.getSerializable(TOKEN, NewToken::class.java),
            ),
        )
    }

    companion object {
        const val ACCOUNT = "ACCOUNT"
        const val TOKEN = "TOKEN"
        const val PARENT_ACTIVITY = "PARENT_ACTIVITY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendTokenBinding.bind(findViewById(R.id.root_layout))
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
        binding.atDisposal.text =
            CurrencyUtil.formatGTU(viewModel.sendTokenData.account.balanceAtDisposal)
        initializeAmount()
        initializeMax()
        initializeAddressBook()
        initializeSend()
        initializeSearchToken()
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
                putExtra(SelectTokenActivity.EXTRA_ACCOUNT, viewModel.sendTokenData.account)
            }
            getResultToken.launch(intent)
        }
    }

    private fun initializeAmount() {
        binding.amount.addTextChangedListener { amountText ->
            val amountString = amountText.toString()
            val token = viewModel.sendTokenData.token

            viewModel.sendTokenData.amount =
                CurrencyUtil.toGTUValue(amountString, token) ?: BigInteger.ZERO

            if (token is CCDToken && token.eurPerMicroCcd != null) {
                binding.eurRate.isVisible = true
                binding.eurRate.text =
                    getString(
                        R.string.cis_estimated_eur_rate,
                        CurrencyUtil.toEURRate(
                            viewModel.sendTokenData.amount,
                            token.eurPerMicroCcd,
                        )
                    )
            } else {
                binding.eurRate.isVisible = false
            }

            if (amountString.isEmpty()) {
                binding.balanceSymbol.alpha = 0.5f
            } else {
                binding.balanceSymbol.alpha = 1f
            }
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
            binding.amount.setText(
                CurrencyUtil.formatGTU(
                    viewModel.sendTokenData.max ?: BigInteger.ZERO,
                    viewModel.sendTokenData.token!!
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
            intent.putExtra(
                RecipientListActivity.EXTRA_SENDER_ACCOUNT,
                viewModel.sendTokenData.account
            )
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
            result.data
                ?.takeIf { result.resultCode == Activity.RESULT_OK }
                ?.getSerializable(
                    SelectTokenActivity.EXTRA_SELECTED_TOKEN,
                    NewToken::class.java,
                )
                ?.let(viewModel.chooseToken::postValue)
        }

    private fun clearMemo() =
        handleMemo("")

    private fun handleMemo(memoText: String) {
        if (memoText.isNotEmpty()) {
            viewModel.setMemoText(memoText)
            setMemoText(memoText)
        } else {
            viewModel.setMemoText(null)
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
                if (token is NewContractToken && token.isUnique)
                    token.metadata?.name ?: ""
                else
                    token.symbol
            if (token is NewContractToken && token.isUnique && token.balance.signum() > 0) {
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
            binding.sendAllButton.isEnabled = token !is CCDToken
            binding.balanceSymbol.text = token.symbol

            if (token !is CCDToken) {
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
            binding.amountError.isVisible = !viewModel.hasEnoughFunds()

            enableSend()
        }
        viewModel.errorInt.observe(this) {
            Toast.makeText(this, getString(it), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setTokenIcon(token: NewToken) {
        val tokenMetadata = token.metadata
        if (tokenMetadata?.thumbnail != null && !tokenMetadata.thumbnail.url.isNullOrBlank()) {
            Glide.with(this)
                .load(tokenMetadata.thumbnail.url)
                .override(resources.getDimensionPixelSize(R.dimen.cis_token_icon_size))
                .placeholder(ThemedCircularProgressDrawable(this))
                .error(R.drawable.mw24_ic_token_placeholder)
                .fitCenter()
                .into(binding.tokenIcon)
        } else if (token is CCDToken) {
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
