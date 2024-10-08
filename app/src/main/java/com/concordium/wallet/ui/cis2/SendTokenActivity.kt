package com.concordium.wallet.ui.cis2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import com.concordium.wallet.Constants
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivitySendTokenBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.SendTokenViewModel.Companion.SEND_TOKEN_DATA
import com.concordium.wallet.ui.recipient.recipientlist.RecipientListActivity
import com.concordium.wallet.ui.scanqr.ScanQRActivity
import com.concordium.wallet.ui.transaction.sendfunds.AddMemoActivity
import com.concordium.wallet.util.CBORUtil
import com.concordium.wallet.util.KeyboardUtil
import com.concordium.wallet.util.getSerializable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.math.BigInteger

class SendTokenActivity : BaseActivity(R.layout.activity_send_token, R.string.cis_send_funds) {
    private lateinit var binding: ActivitySendTokenBinding
    private val viewModel: SendTokenViewModel by viewModels()
    private val viewModelTokens: TokensViewModel by viewModels()
    private var selectTokenBottomSheet: SelectTokenBottomSheet? = null

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
        binding.amount.hint = CurrencyUtil.formatGTU(BigInteger.ZERO, false)
        binding.atDisposal.text = CurrencyUtil.formatGTU(
            viewModel.sendTokenData.account?.balanceAtDisposal ?: BigInteger.ZERO,
            true
        )
        initializeAmount()
        initializeMax()
        initializeReceiver()
        initializeAddressBook()
        initializeScanQrCode()
        initializeSend()
        initializeSearchToken()
        viewModel.getGlobalInfo()
        viewModel.getAccountBalance()
    }

    private fun initializeSend() {
        binding.send.setOnClickListener {
            send()
        }
    }

    private fun send() {
        binding.send.isEnabled = false
        gotoReceipt()
    }

    private fun initializeSearchToken() {
        binding.token.setOnClickListener {
            selectTokenBottomSheet = SelectTokenBottomSheet.newInstance(viewModel, viewModelTokens)
            selectTokenBottomSheet?.show(supportFragmentManager, "")
        }
    }

    private fun initializeAmount() {
        binding.amount.addTextChangedListener {
            viewModel.sendTokenData.amount =
                CurrencyUtil.toGTUValue(it.toString(), viewModel.sendTokenData.token)
                    ?: BigInteger.ZERO
            viewModel.loadTransactionFee()
            enableSend()
        }
        binding.amount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && viewModel.sendTokenData.amount.signum() == 0)
                binding.amount.setText("")
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
                    viewModel.sendTokenData.max ?: BigInteger.ZERO, false, decimals
                )
            )
            enableSend()
        }
    }

    private fun enableSend(): Boolean {
        binding.send.isEnabled = viewModel.canSend
        return binding.send.isEnabled
    }

    private fun initializeReceiver() {
        binding.receiver.doOnTextChanged { text, _, _, _ ->
            onReceiverEntered(text?.toString() ?: "")
        }
    }

    private fun initializeAddressBook() {
        binding.addressBook.setOnClickListener {
            val intent = Intent(this, RecipientListActivity::class.java)
            intent.putExtra(RecipientListActivity.EXTRA_SELECT_RECIPIENT_MODE, true)
            intent.putExtra(RecipientListActivity.EXTRA_SHIELDED, viewModel.sendTokenData.account)
            intent.putExtra(RecipientListActivity.EXTRA_ACCOUNT, viewModel.sendTokenData.account)
            getResultRecipient.launch(intent)
        }
    }

    private fun initializeScanQrCode() {
        binding.scanQr.setOnClickListener {
            val intent = Intent(this, ScanQRActivity::class.java)
            intent.putExtra(Constants.Extras.EXTRA_ADD_CONTACT, true)
            getResultScanQr.launch(intent)
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
                    binding.receiver.setText(recipient.address)
                    if (recipient.name.isNotEmpty()) {
                        onReceiverNameFound(recipient.name)
                    }
                }
            }
        }

    private val getResultScanQr =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result
                .takeIf { it.resultCode == Activity.RESULT_OK }
                ?.data?.extras
                ?.let(ScanQRActivity::getScannedQrContent)
                ?.also { scannedQrContent ->
                    binding.receiver.setText(scannedQrContent)
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

    private fun onReceiverEntered(input: String) {
        viewModel.onReceiverEntered(input)
        binding.receiverName.isVisible = false
        binding.or.isInvisible = input.isNotEmpty()
        enableSend()
    }

    private fun onReceiverNameFound(name: String) {
        viewModel.onReceiverNameFound(name)
        binding.receiverName.isVisible = true
        binding.receiverName.text = name
    }

    private fun initObservers() {
        viewModel.waiting.observe(this) { waiting ->
            showWaiting(waiting)
        }
        viewModel.chooseToken.observe(this) { token ->
            selectTokenBottomSheet?.dismiss()
            selectTokenBottomSheet = null
            binding.balanceTitle.text =
                if (token.isUnique)
                    getString(R.string.cis_token_quantity)
                else
                    getString(R.string.cis_token_balance, token.symbol).trim()
            val decimals = token.decimals
            binding.balance.text =
                CurrencyUtil.formatGTU(token.balance, token.isCcd, decimals)
            binding.token.text =
                if (token.isUnique)
                    token.name
                else
                    token.symbol
            if (token.isUnique && token.balance.signum() > 0) {
                // For owned NFTs, prefill the amount (quantity) which is 1
                // for smoother experience.
                binding.amount.setText("1")
            } else {
                // Clearing the text reveals the "0,00" hint.
                binding.amount.text.clear()
            }
            binding.amount.decimals = decimals
            // For non-CCD tokens Max is always available.
            binding.sendAllButton.isEnabled = !token.isCcd

            if (!token.isCcd) {
                binding.addMemo.visibility = View.GONE
            } else {
                binding.addMemo.visibility = View.VISIBLE
                binding.addMemo.setOnClickListener {
                    if (viewModel.showMemoWarning()) {
                        val builder = MaterialAlertDialogBuilder(this)
                        builder.setTitle(getString(R.string.transaction_memo_warning_title))
                        builder.setMessage(getString(R.string.transaction_memo_warning_text))
                        builder.setNegativeButton(getString(R.string.transaction_memo_warning_dont_show)) { _, _ ->
                            viewModel.dontShowMemoWarning()
                            goToEnterMemo()
                        }
                        builder.setPositiveButton(getString(R.string.transaction_memo_warning_ok)) { _, _ ->
                            goToEnterMemo()
                        }
                        builder.setCancelable(true)
                        builder.create().show()
                    } else {
                        goToEnterMemo()
                    }
                }
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
                        getString(
                            R.string.amount,
                            CurrencyUtil.formatGTU(fee, true)
                        )
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
