package com.concordium.wallet.ui.account.accountdetails

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityAccountSettingsBinding
import com.concordium.wallet.databinding.DialogEdittextBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.more.export.ExportAccountKeysActivity
import com.concordium.wallet.ui.more.export.ExportTransactionLogActivity
import com.concordium.wallet.util.getSerializable
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AccountSettingsActivity : BaseActivity(
    R.layout.activity_account_settings,
    R.string.account_settings_title,
) {
    private lateinit var binding: ActivityAccountSettingsBinding
    private lateinit var viewModel: AccountSettingsViewModel

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
        const val EXTRA_SHIELDED = "EXTRA_SHIELDED"
        const val EXTRA_CONTINUE_TO_SHIELD_INTRO = "EXTRA_CONTINUE_TO_SHIELD_INTRO"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountSettingsBinding.bind(findViewById(R.id.root_layout))
        initializeViewModel()
        viewModel.initialize(
            intent.getSerializable(EXTRA_ACCOUNT, Account::class.java),
            intent.getBooleanExtra(EXTRA_SHIELDED, false)
        )
        hideActionBarBack(isVisible = true)
        initViews()
        initObservers()
        val continueToShieldIntro = intent.extras!!.getBoolean(EXTRA_CONTINUE_TO_SHIELD_INTRO)
        if (continueToShieldIntro) {
            startShieldedIntroFlow()
        }
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AccountSettingsViewModel::class.java]
    }

    private fun initObservers() {
        viewModel.accountUpdated.observe(this) {

        }
    }

    private fun initViews() {
        binding.transferFilter.setOnClickListener {
            gotoTransferFilters(viewModel.account)
        }
        binding.releaseSchedule.setOnClickListener {
            gotoAccountReleaseSchedule(viewModel.account, viewModel.isShielded)
        }
        binding.exportKey.setOnClickListener {
            exportKey()
        }
        binding.exportTransactionLog.setOnClickListener {
            exportTransactionLog()
        }
        binding.changeName.setOnClickListener {
            showChangeNameDialog()
        }

        binding.transferFilter.visibility = if (viewModel.isShielded) View.GONE else View.VISIBLE
        binding.releaseSchedule.visibility = if (viewModel.isShielded) View.GONE else View.VISIBLE
    }

    private fun gotoTransferFilters(account: Account) {
        val intent = Intent(this, AccountTransactionsFiltersActivity::class.java)
        intent.putExtra(AccountDetailsActivity.EXTRA_ACCOUNT, account)
        startActivity(intent)
    }

    private fun gotoAccountReleaseSchedule(account: Account, isShielded: Boolean) {
        val intent = Intent(this, AccountReleaseScheduleActivity::class.java)
        intent.putExtra(AccountDetailsActivity.EXTRA_ACCOUNT, account)
        intent.putExtra(AccountDetailsActivity.EXTRA_SHIELDED, isShielded)
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
        }
        builder.setNegativeButton(getString(R.string.account_details_change_name_popup_cancel)) { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
        input.requestFocus()
    }

    private fun startShieldedIntroFlow() {
    }
}
