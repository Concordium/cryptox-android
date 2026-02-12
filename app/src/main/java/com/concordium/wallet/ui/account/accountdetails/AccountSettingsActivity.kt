package com.concordium.wallet.ui.account.accountdetails

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityAccountSettingsBinding
import com.concordium.wallet.databinding.DialogEdittextBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.more.export.ExportAccountKeysActivity
import com.concordium.wallet.ui.more.export.ExportTransactionLogActivity
import com.concordium.wallet.uicore.toast.ToastType
import com.concordium.wallet.uicore.toast.showCustomToast
import com.concordium.wallet.util.ClipboardUtil
import com.concordium.wallet.util.getSerializable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.math.BigInteger

class AccountSettingsActivity : BaseActivity(
    R.layout.activity_account_settings,
    R.string.account_settings_title,
) {
    private lateinit var binding: ActivityAccountSettingsBinding
    private lateinit var viewModel: AccountSettingsViewModel

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountSettingsBinding.bind(findViewById(R.id.root_layout))
        initializeViewModel()
        viewModel.initialize(
            account = intent.getSerializable(EXTRA_ACCOUNT, Account::class.java),
        )
        hideActionBarBack(isVisible = true)
        initViews()
        initObservers()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AccountSettingsViewModel::class.java]
    }

    private fun initObservers() {
        viewModel.accountUpdated.observe(this) { updated ->
            if (updated == true) {
                binding.apply {
                    accountName.text = viewModel.account.getAccountName()
                    accountAddress.text = viewModel.account.address
                    accountIdentity.text = viewModel.identityName
                    totalBalance.text = getString(
                        R.string.amount,
                        CurrencyUtil.formatGTU(
                            value = viewModel.account.balance
                        )
                    )
                    atDisposalBalance.text = getString(
                        R.string.amount,
                        CurrencyUtil.formatGTU(
                            value = viewModel.account.balanceAtDisposal
                        )
                    )
                    earningBalance.text = getString(
                        R.string.amount,
                        CurrencyUtil.formatGTU(
                            value = viewModel.account.delegation?.stakedAmount ?: BigInteger.ZERO
                        )
                    )
                    scheduledBalance.text = getString(
                        R.string.amount,
                        CurrencyUtil.formatGTU(
                            value = viewModel.account.releaseSchedule?.total ?: BigInteger.ZERO
                        )
                    )
                }
            }
        }
    }

    private fun initViews() {
        binding.releaseSchedule.setOnClickListener {
            gotoAccountReleaseSchedule(viewModel.account)
        }
        binding.exportKey.setOnClickListener {
            exportKey()
        }
        binding.exportTransactionLog.isVisible = viewModel.canExportTransactionLogs
        binding.exportTransactionLog.setOnClickListener {
            exportTransactionLog()
        }
        binding.changeName.setOnClickListener {
            showChangeNameDialog()
        }
        binding.copyAddressButton.setOnClickListener {
            copyAddress(viewModel.account.address)
        }
    }

    private fun gotoAccountReleaseSchedule(account: Account) {
        val intent = Intent(this, AccountReleaseScheduleActivity::class.java)
        intent.putExtra(AccountReleaseScheduleActivity.EXTRA_ACCOUNT, account)
        startActivity(intent)
    }

    private fun exportKey() {
        val intent = Intent(this, ExportAccountKeysActivity::class.java)
        intent.putExtra(ExportAccountKeysActivity.EXTRA_ACCOUNT, viewModel.account)
        startActivity(intent)
    }

    private fun exportTransactionLog() {
        val intent = Intent(this, ExportTransactionLogActivity::class.java)
        intent.putExtra(ExportTransactionLogActivity.EXTRA_ACCOUNT, viewModel.account)
        startActivity(intent)
    }

    private fun showChangeNameDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(getString(R.string.account_details_change_name_popup_title))
        builder.setMessage(getString(R.string.account_details_change_name_popup_subtitle))
        val view = DialogEdittextBinding.inflate(LayoutInflater.from(builder.context))
        val input = view.inputEdittext
        input.setText(viewModel.account.name)
        input.hint = getString(R.string.account_details_change_name_hint)
        builder.setView(view.root)
        builder.setPositiveButton(getString(R.string.account_details_change_name_popup_save)) { _, _ ->
            viewModel.changeAccountName(input.text.toString())
            showCustomToast(title = getString(R.string.account_settings_name_changed))
        }
        builder.setNegativeButton(getString(R.string.account_details_change_name_popup_cancel)) { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
        input.requestFocus()
    }

    private fun copyAddress(address: String) {
        ClipboardUtil.copyToClipboard(
            context = this,
            text = address
        )
        showCustomToast(
            title = getString(R.string.account_settings_address_copied),
            toastType = ToastType.INFO
        )
    }
}
