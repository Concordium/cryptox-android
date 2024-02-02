package com.concordium.wallet.ui.transaction.sendfunds

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.data.model.ShieldedAccountEncryptionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivitySendFundsBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.common.failed.FailedActivity
import com.concordium.wallet.ui.common.failed.FailedViewModel
import com.concordium.wallet.ui.recipient.recipientlist.RecipientListActivity
import com.concordium.wallet.ui.transaction.sendfundsconfirmed.SendFundsConfirmedActivity
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.CBORUtil
import com.concordium.wallet.util.getSerializable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.DecimalFormatSymbols

class SendFundsActivity : BaseActivity(
    R.layout.activity_send_funds,
    R.string.send_funds_title
), AuthDelegate by AuthDelegateImpl() {

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
        const val EXTRA_SHIELDED = "EXTRA_SHIELDED"
        const val EXTRA_RECIPIENT = "EXTRA_RECIPIENT"
        const val EXTRA_MEMO = "EXTRA_MEMO"
    }

    private lateinit var viewModel: SendFundsViewModel
    private val binding by lazy {
        ActivitySendFundsBinding.bind(findViewById(R.id.root_layout))
    }

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val account = intent.extras!!.getSerializable(EXTRA_ACCOUNT) as Account
        val isShielded = intent.extras!!.getBoolean(EXTRA_SHIELDED)
        initializeViewModel()
        viewModel.initialize(account, isShielded)
        handleRecipient(intent)
        handleMemo(intent)
        initViews()

        closeBtn?.visibility = View.VISIBLE
        closeBtn?.setOnClickListener {
            finish()
        }
    }

    private fun handleRecipient(intent: Intent?) {
        val recipient = intent?.getSerializableExtra(EXTRA_RECIPIENT) as? Recipient
        if (recipient != null) {
            handleRecipient(recipient)
        }
    }

    private fun handleRecipient(recipient: Recipient) {
        viewModel.selectedRecipient = recipient
        updateConfirmButton()
        if (viewModel.account.id == recipient.id) {
            binding.selectRecipientLayout.visibility = View.GONE
            setActionBarTitle(if (viewModel.isShielded) R.string.send_funds_unshield_title else R.string.send_funds_shield_title)
        }
    }

    private fun handleMemo(intent: Intent?) {
        intent?.let {
            if (it.hasExtra(EXTRA_MEMO)) {
                handleMemo(it.getStringExtra(EXTRA_MEMO))
            }
        }
    }

    private fun handleMemo(memoText: String?) {
        if (!memoText.isNullOrEmpty()) {
            viewModel.setMemo(CBORUtil.encodeCBOR(memoText))
            setMemoText(memoText)
        } else {
            viewModel.setMemo(null)
            setMemoText("")
        }
    }

    private fun setMemoText(txt: String) {
        if (txt.isNotEmpty()) {
            binding.memoLayout.visibility = View.VISIBLE
            binding.memoTextview.text = txt
            binding.memoContainer.setText(R.string.send_funds_optional_edit_memo)
        } else {
            binding.memoLayout.visibility = View.GONE
            binding.memoTextview.text = ""
            binding.memoContainer.setText(R.string.send_funds_optional_add_memo)
        }
    }

    //endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[SendFundsViewModel::class.java]

        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(isWaiting())
            }
        }
        viewModel.waitingReceiverAccountPublicKeyLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(isWaiting())
            }
        }
        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
        viewModel.showAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showAuthentication(
                        activity = this@SendFundsActivity,
                        onAuthenticated = viewModel::continueWithPassword
                    )
                }
            }
        })
        viewModel.gotoSendFundsConfirmLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    gotoSendFundsConfirm()
                }
            }
        })
        viewModel.gotoFailedLiveData.observe(
            this,
            object : EventObserver<Pair<Boolean, BackendError?>>() {
                override fun onUnhandledEvent(value: Pair<Boolean, BackendError?>) {
                    if (value.first) {
                        gotoFailed(value.second)
                    }
                }
            })
        viewModel.transactionFeeLiveData.observe(this) { value ->
            value?.let {
                binding.feeInfoTextview.visibility = View.VISIBLE
                binding.feeInfoTextview.text = getString(
                    R.string.send_funds_fee_info, CurrencyUtil.formatGTU(value)
                )
                updateConfirmButton()
            }
        }
        viewModel.recipientLiveData.observe(this) { value -> showRecipient(value) }
    }

    private fun initViews() {
        binding.progress.progressLayout.visibility = View.GONE
        binding.errorTextview.visibility = View.INVISIBLE
        binding.amountEdittext.afterTextChanged {
            updateConfirmButton()
            updateAmountEditText()
        }
        updateAmountEditText()
        binding.amountEdittext.setOnEditorActionListener { textView, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    if (textView.text.isNotEmpty())
                        viewModel.sendFunds(binding.amountEdittext.text.toString())
                    true
                }

                else -> false
            }
        }

        binding.memoTextview.setOnClickListener {
            gotoEnterMemo()
        }

        if (viewModel.isTransferToSameAccount()) {
            binding.memoContainer.visibility = View.GONE
        } else {
            binding.memoContainer.visibility = View.VISIBLE

            binding.memoContainer.setOnClickListener {
                if (viewModel.showMemoWarning()) {
                    val builder = MaterialAlertDialogBuilder(this)
                    builder.setTitle(getString(R.string.transaction_memo_warning_title))
                    builder.setMessage(getString(R.string.transaction_memo_warning_text))
                    builder.setNegativeButton(getString(R.string.transaction_memo_warning_dont_show)) { _, _ ->
                        viewModel.dontShowMemoWarning()
                        gotoEnterMemo()
                    }
                    builder.setPositiveButton(getString(R.string.transaction_memo_warning_ok)) { _, _ ->
                        gotoEnterMemo()
                    }
                    builder.setCancelable(true)
                    builder.create().show()
                } else {
                    gotoEnterMemo()
                }
            }
        }

        binding.amountEdittext.requestFocus()

        binding.selectRecipientLayout.setText(if (viewModel.isShielded) R.string.send_funds_select_recipient_or_unshield_amount else R.string.send_funds_select_recipient_or_shield_amount)

        binding.selectRecipientLayout.setOnClickListener {
            gotoSelectRecipient()
        }

        binding.confirmButton.setOnClickListener {
            if (viewModel.selectedRecipient != null) {
                viewModel.sendFunds(binding.amountEdittext.text.toString())
            } else {
                showError(R.string.no_recipient_error)
            }
        }

        binding.balanceTotalTextview.text = CurrencyUtil.formatGTU(
            viewModel.account.getAtDisposalWithoutStakedOrScheduled(viewModel.account.totalUnshieldedBalance),
            withGStroke = true
        )
        binding.shieldedTotalTextview.text =
            CurrencyUtil.formatGTU(viewModel.account.totalShieldedBalance, withGStroke = true)
        binding.shieldedTotalTextview.visibility =
            if (viewModel.account.encryptedBalanceStatus == ShieldedAccountEncryptionStatus.DECRYPTED || viewModel.account.encryptedBalanceStatus == ShieldedAccountEncryptionStatus.PARTIALLYDECRYPTED) View.VISIBLE else View.GONE
        binding.shieldedLockPlus.visibility = binding.shieldedTotalTextview.visibility
        binding.shieldedLockContainer.visibility =
            if (viewModel.account.encryptedBalanceStatus != ShieldedAccountEncryptionStatus.DECRYPTED) View.VISIBLE else View.GONE
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun showWaiting(waiting: Boolean) {
        binding.progress.progressLayout.isVisible = waiting
        // Update button enabled state, because it is dependant on waiting state
        updateConfirmButton()
    }

    private val getResultRecipient =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getSerializable(
                    RecipientListActivity.EXTRA_RECIPIENT,
                    Recipient::class.java
                )?.let { recipient ->
                    handleRecipient(recipient)
                }
            }
        }

    private fun gotoSelectRecipient() {
        val intent = Intent(this, RecipientListActivity::class.java)
        intent.putExtra(RecipientListActivity.EXTRA_SELECT_RECIPIENT_MODE, true)
        intent.putExtra(RecipientListActivity.EXTRA_SHIELDED, viewModel.isShielded)
        intent.putExtra(RecipientListActivity.EXTRA_ACCOUNT, viewModel.account)
        getResultRecipient.launch(intent)
    }

    private val getResultMemo =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getStringExtra(AddMemoActivity.EXTRA_MEMO)?.let { memoText ->
                    handleMemo(memoText)
                }
            }
        }

    private fun gotoEnterMemo() {
        val intent = Intent(this, AddMemoActivity::class.java)
        intent.putExtra(AddMemoActivity.EXTRA_MEMO, viewModel.getClearTextMemo())
        getResultMemo.launch(intent)
    }

    private fun gotoSendFundsConfirm() {
        val transfer = viewModel.newTransfer
        val recipient = viewModel.selectedRecipient
        if (transfer != null && recipient != null) {
            val intent = Intent(this, SendFundsConfirmedActivity::class.java)
            intent.putExtra(SendFundsConfirmedActivity.EXTRA_TRANSFER, transfer)
            intent.putExtra(SendFundsConfirmedActivity.EXTRA_RECIPIENT, recipient)
            startActivity(intent)
        }
    }

    private fun gotoFailed(error: BackendError?) {
        val intent = Intent(this, FailedActivity::class.java)
        intent.putExtra(FailedActivity.EXTRA_SOURCE, FailedViewModel.Source.Transfer)
        error?.let {
            intent.putExtra(FailedActivity.EXTRA_ERROR, it)
        }
        startActivity(intent)
    }

    private fun showRecipient(recipient: Recipient?) {
        if (recipient == null) {
            binding.selectRecipientLayout.text = ""
            binding.confirmButton.setText(R.string.send_funds_confirm)
        } else {
            if (viewModel.isShielded) {
                if (viewModel.isTransferToSameAccount()) {
                    binding.selectRecipientLayout.setText(R.string.send_funds_recipient_unshield)
                    binding.confirmButton.setText(R.string.send_funds_confirm_unshield)
                } else {
                    binding.selectRecipientLayout.text = recipient.name
                    binding.confirmButton.setText(R.string.send_funds_confirm_send_shielded)
                }
            } else {
                if (viewModel.isTransferToSameAccount()) {
                    binding.selectRecipientLayout.setText(R.string.send_funds_recipient_shield)
                    binding.confirmButton.setText(R.string.send_funds_confirm_shield)
                } else {
                    binding.selectRecipientLayout.text = recipient.name
                    binding.confirmButton.setText(R.string.send_funds_confirm)
                }
            }
        }
    }

    private fun isWaiting(): Boolean {
        var waiting = false
        viewModel.waitingLiveData.value?.let {
            if (it) {
                waiting = true
            }
        }
        viewModel.waitingReceiverAccountPublicKeyLiveData.value?.let {
            if (it) {
                waiting = true
            }
        }
        return waiting
    }

    private fun updateConfirmButton(): Boolean {
        val hasSufficientFunds = viewModel.hasSufficientFunds(binding.amountEdittext.text.toString())
        binding.errorTextview.visibility = if (hasSufficientFunds) View.INVISIBLE else View.VISIBLE
        val enabled = if (isWaiting()) false else {
            binding.amountEdittext.text.isNotEmpty() &&
                    viewModel.selectedRecipient != null &&
                    viewModel.transactionFeeLiveData.value != null &&
                    hasSufficientFunds
        }
        binding.confirmButton.isEnabled = enabled
        return enabled
    }

    private fun updateAmountEditText() {
        if (binding.amountEdittext.text.isNotEmpty()) {
            // Only setting this (to one char) to have the width being smaller
            // Width is WRAP_CONTENT and hint text count towards this
            binding.amountEdittext.hint = "0"
            binding.amountEdittext.gravity = Gravity.CENTER
        } else {
            binding.amountEdittext.hint = "0${DecimalFormatSymbols.getInstance().decimalSeparator}00"
            binding.amountEdittext.gravity = Gravity.NO_GRAVITY
        }
    }

    private fun createConfirmString(): String? {
        val amount = viewModel.getAmount()
        val cost = viewModel.transactionFeeLiveData.value
        val recipient = viewModel.selectedRecipient
        if (amount == null || cost == null || recipient == null) {
            showError(R.string.app_error_general)
            return null
        }
        val amountString = CurrencyUtil.formatGTU(amount, withGStroke = true)
        val costString = CurrencyUtil.formatGTU(cost, withGStroke = true)

        val memoText = if (viewModel.getClearTextMemo()
                .isNullOrEmpty()
        ) "" else getString(R.string.send_funds_confirmation_memo, viewModel.getClearTextMemo())

        return if (viewModel.isShielded) {
            if (viewModel.isTransferToSameAccount()) {
                getString(
                    R.string.send_funds_confirmation_unshield,
                    amountString,
                    recipient.name,
                    costString,
                    memoText
                )
            } else {
                getString(
                    R.string.send_funds_confirmation_send_shielded,
                    amountString,
                    recipient.name,
                    costString,
                    memoText
                )
            }
        } else {
            if (viewModel.isTransferToSameAccount()) {
                getString(
                    R.string.send_funds_confirmation_shield,
                    amountString,
                    recipient.name,
                    costString,
                    memoText
                )
            } else {
                getString(
                    R.string.send_funds_confirmation,
                    amountString,
                    recipient.name,
                    costString,
                    memoText
                )
            }
        }
    }

    //endregion
}
