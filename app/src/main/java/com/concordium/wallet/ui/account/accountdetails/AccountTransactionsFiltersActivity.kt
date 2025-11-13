package com.concordium.wallet.ui.account.accountdetails

import android.content.Intent
import android.os.Bundle
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.Session
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityAccountTransactionFiltersBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.more.export.ExportTransactionLogActivity

class AccountTransactionsFiltersActivity : BaseActivity(
    R.layout.activity_account_transaction_filters,
) {

    private val session: Session = App.appCore.session
    private val binding by lazy {
        ActivityAccountTransactionFiltersBinding.bind(findViewById(R.id.root_layout))
    }
    private lateinit var mAccount: Account

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAccount = intent.extras!!.getSerializable(EXTRA_ACCOUNT) as Account
        hideActionBarBack(isVisible = true)

        initViews()
    }

    override fun onResume() {
        super.onResume()

        binding.apply {
            filterShowRewardsSwitch.isChecked = session.getHasShowRewards(mAccount.id)
            filterShowFinalizationsRewardsSwitch.isChecked =
                session.getHasShowFinalizationRewards(mAccount.id)
            filterShowFinalizationsRewardsSwitch.isEnabled = filterShowRewardsSwitch.isChecked

            filterShowRewardsSwitch.setOnCheckedChangeListener { _, isChecked ->
                session.setHasShowRewards(mAccount.id, isChecked)
                if (!isChecked) {
                    filterShowFinalizationsRewardsSwitch.isEnabled = false
                    filterShowFinalizationsRewardsSwitch.isChecked = false
                    session.setHasShowFinalizationRewards(mAccount.id, false)
                } else {
                    filterShowFinalizationsRewardsSwitch.isEnabled = true
                }
            }
            filterShowFinalizationsRewardsSwitch.setOnCheckedChangeListener { _, isChecked ->
                session.setHasShowFinalizationRewards(mAccount.id, isChecked)
            }

            filterShowRewardsTextView.setOnClickListener {
                filterShowRewardsSwitch.performClick()
            }

            filterShowFinalizationsRewardsTextView.setOnClickListener {
                if (filterShowRewardsSwitch.isChecked) {
                    filterShowFinalizationsRewardsSwitch.performClick()
                }
            }
        }
    }

    private fun initViews() {
        setActionBarTitle(R.string.account_transaction_filters_title)
        binding.exportTransactionLog.setOnClickListener { exportTransactionLog() }
    }

    private fun exportTransactionLog() {
        val intent = Intent(this, ExportTransactionLogActivity::class.java)
        intent.putExtra(ExportTransactionLogActivity.EXTRA_ACCOUNT, mAccount)
        startActivity(intent)
    }
}
