package com.concordium.wallet.ui.cis2.send

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.ContractToken
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.FragmentSendTokenBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.MemoNoticeDialog
import com.concordium.wallet.ui.cis2.TokenIconView
import com.concordium.wallet.ui.recipient.recipientlist.RecipientListActivity
import com.concordium.wallet.ui.transaction.sendfunds.AddMemoActivity
import com.concordium.wallet.util.KeyboardUtil
import com.concordium.wallet.util.KeyboardUtil.showKeyboard
import com.concordium.wallet.util.getSerializable
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.math.BigInteger

class SendTokenFragment : Fragment() {

    private lateinit var binding: FragmentSendTokenBinding
    private val viewModel: SendTokenViewModel by viewModel {
        parametersOf(
            ViewModelProvider(requireActivity())[MainViewModel::class.java]
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSendTokenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        initObservers()
        initFragmentListener()
    }

    override fun onResume() {
        super.onResume()
        enableSend()
    }

    private fun initViews() {
        binding.amount.hint = BigInteger.ZERO.toString()
        initializeAmount()
        initializeMax()
        initializeAddressBook()
        initializeSend()
        initializeSearchToken()
        initializeMemo()
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
            val intent = Intent(requireActivity(), SelectTokenActivity::class.java).apply {
                putExtra(
                    SelectTokenActivity.EXTRA_ACCOUNT_ADDRESS,
                    viewModel.sendTokenData.account.address
                )
            }
            getResultToken.launch(intent)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initializeAmount() {
        binding.amount.addTextChangedListener { amountText ->
            val amountString = amountText.toString()
            val token = viewModel.sendTokenData.token

            viewModel.sendTokenData.amount =
                CurrencyUtil.toGTUValue(amountString, token) ?: BigInteger.ZERO

            if (amountString.isEmpty()) {
                binding.balanceSymbol.alpha = 0.5f
            } else {
                binding.balanceSymbol.alpha = 1f
            }
            viewModel.loadFee()
            enableSend()
            setEstimatedAmountInEur()
        }
        binding.amount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (viewModel.sendTokenData.amount.signum() == 0) {
                    binding.amount.setText("")
                }
                showKeyboard(requireActivity(), binding.amount)
            }
        }
        binding.amount.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    KeyboardUtil.hideKeyboard(requireActivity())
                    if (enableSend())
                        send()
                    true
                }

                else -> false
            }
        }
        binding.balanceSymbol.setOnClickListener {
            showKeyboard(requireActivity(), binding.amount)
        }
    }

    private fun initializeMax() {
        binding.sendAllButton.setOnClickListener {
            binding.amount.setText(
                CurrencyUtil.formatGTU(
                    value = viewModel.sendTokenData.maxAmount ?: BigInteger.ZERO,
                    token = viewModel.sendTokenData.token,
                    withCommas = false,
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
        resetAddressBook()
        binding.recipientLayout.setOnClickListener {
            val intent = Intent(requireActivity(), RecipientListActivity::class.java)
            intent.putExtra(RecipientListActivity.EXTRA_SHIELDED, viewModel.sendTokenData.account)
            intent.putExtra(
                RecipientListActivity.EXTRA_SENDER_ACCOUNT,
                viewModel.sendTokenData.account
            )
            getResultRecipient.launch(intent)
        }
    }

    private fun initializeMemo() {
        setMemoText("")
        binding.memoLayout.setOnClickListener {
            if (viewModel.showMemoWarning()) {
                MemoNoticeDialog().showSingle(
                    parentFragmentManager,
                    MemoNoticeDialog.TAG
                )
            } else {
                goToEnterMemo()
            }
        }
    }

    private fun goToEnterMemo() {
        val intent = Intent(requireActivity(), AddMemoActivity::class.java)
        intent.putExtra(AddMemoActivity.EXTRA_MEMO, viewModel.getMemoText())
        getResultMemo.launch(intent)
    }

    private val getResultMemo =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result
                .data
                ?.takeIf { result.resultCode == Activity.RESULT_OK }
                ?.getStringExtra(AddMemoActivity.EXTRA_MEMO)
                ?.also(::handleMemo)
        }

    private val getResultRecipient =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result
                .data
                ?.takeIf { result.resultCode == Activity.RESULT_OK }
                ?.getSerializable(
                    RecipientListActivity.EXTRA_RECIPIENT,
                    Recipient::class.java
                )?.also { recipient ->
                    viewModel.onReceiverEntered(recipient.address)
                    binding.recipientPlaceholder.visibility = View.GONE
                    binding.recipientAddress.visibility = View.VISIBLE

                    if (recipient.name.isNotEmpty()) {
                        viewModel.onReceiverNameFound(recipient.name)
                        binding.recipientAddress.text = getString(
                            R.string.cis_recipient_address_with_name,
                            recipient.name,
                            recipient.address
                        )
                    } else {
                        binding.recipientAddress.text = recipient.address
                    }
                }
        }

    private val getResultToken =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data
                ?.takeIf { result.resultCode == Activity.RESULT_OK }
                ?.getSerializable(
                    SelectTokenActivity.EXTRA_SELECTED_TOKEN,
                    Token::class.java,
                )
                ?.also(viewModel::onTokenSelected)
        }

    private fun handleMemo(memoText: String) {
        if (memoText.isNotEmpty()) {
            viewModel.setMemoText(memoText)
            setMemoText(memoText)
        } else {
            viewModel.setMemoText(null)
            setMemoText("")
        }
    }

    private fun setMemoText(memoText: String) = with(binding) {
        if (memoText.isNotEmpty()) {
            addMemoPlaceholder.visibility = View.GONE
            memo.visibility = View.VISIBLE
            memo.text = memoText
        } else {
            addMemoPlaceholder.visibility = View.VISIBLE
            memo.visibility = View.GONE
        }
    }

    private fun resetAddressBook() {
        with(binding) {
            recipientAddress.text = ""
            recipientAddress.visibility = View.GONE
            recipientPlaceholder.visibility = View.VISIBLE
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initObservers() {
        viewModel.accountUpdated.collectWhenStarted(viewLifecycleOwner) {
            setMemoText("")
            binding.recipientPlaceholder.isVisible = true
            binding.recipientAddress.isVisible = false
        }

        viewModel.waiting.observe(viewLifecycleOwner, ::showWaiting)

        viewModel.token.observe(viewLifecycleOwner) { token ->
            TokenIconView(binding.tokenIcon).showTokenIcon(token)

            val decimals = token.decimals
            binding.balance.text = CurrencyUtil.formatGTU(token.balance, decimals)
            binding.token.text =
                if (token is ContractToken && token.isUnique)
                    token.metadata?.name ?: ""
                else
                    token.symbol
            if (token is ContractToken && token.isUnique && token.balance.signum() > 0) {
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

            binding.memoLayout.isVisible = token !is ContractToken
            setMemoText(viewModel.getMemoText() ?: "")

            binding.atDisposalTitle.isVisible = token is CCDToken
        }

        viewModel.feeReady.observe(viewLifecycleOwner) { fee ->
            // Null value means the fee is outdated.
            binding.fee.text =
                if (fee != null)
                    getString(
                        R.string.cis_estimated_fee,
                        CurrencyUtil.formatGTU(fee)
                    )
                else
                    ""
            binding.sendAllButton.isEnabled =
                viewModel.sendTokenData.token !is CCDToken || fee != null

            enableSend()
        }

        viewModel.errorInt.observe(viewLifecycleOwner) {
            Toast.makeText(requireActivity(), getString(it), Toast.LENGTH_SHORT).show()
        }

        viewModel.tokenEurRate.observe(viewLifecycleOwner) {
            setEstimatedAmountInEur()
        }

        viewModel.recipientError.collectWhenStarted(viewLifecycleOwner) {
            binding.recipientError.apply {
                isVisible = it != -1
                if (isVisible) text = getString(it)
            }
        }

        viewModel.hasEnoughFunds.collectWhenStarted(viewLifecycleOwner) { hasEnoughFunds ->
            binding.amountError.isVisible = !hasEnoughFunds
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setEstimatedAmountInEur() {
        val rate = viewModel.tokenEurRate.value

        if (rate != null) {
            binding.eurRate.text =
                getString(
                    R.string.cis_estimated_eur_rate,
                    CurrencyUtil.toEURRate(
                        viewModel.sendTokenData.amount,
                        rate,
                    )
                )
        } else {
            binding.eurRate.text = ""
        }
    }

    private fun initFragmentListener() {
        parentFragmentManager.setFragmentResultListener(
            MemoNoticeDialog.ACTION_REQUEST,
            viewLifecycleOwner
        ) { _, bundle ->
            val showAgain = MemoNoticeDialog.getResult(bundle)
            if (!showAgain) {
                viewModel.dontShowMemoWarning()
            }
            goToEnterMemo()
        }
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressBar.visibility =
            if (waiting) View.VISIBLE else View.GONE
    }

    private fun gotoReceipt() {
        val intent = Intent(requireActivity(), SendTokenReceiptActivity::class.java)
        intent.putExtra(
            SendTokenReceiptActivity.SEND_TOKEN_DATA,
            viewModel.sendTokenData,
        )
        (activity as BaseActivity).startActivityForResultAndHistoryCheck(intent)
    }
}
