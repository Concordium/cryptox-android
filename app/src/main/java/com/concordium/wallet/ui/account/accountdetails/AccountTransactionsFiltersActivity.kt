package com.concordium.wallet.ui.account.accountdetails

import android.os.Bundle
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.Session
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityAccountTransactionFiltersBinding
import com.concordium.wallet.ui.base.BaseActivity

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

    //region Lifecycle
    // ************************************************************

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAccount = intent.extras!!.getSerializable(EXTRA_ACCOUNT) as Account
        initializeViewModel()
        hideActionBarBack(isVisible = true)
        setActionBarTitle(
            getString(
                R.string.account_transaction_filters_title,
                mAccount.getAccountName()
            )
        )
        initViews()
    }

    override fun onResume() {
        super.onResume()

        binding.filterShowRewards.isChecked = session.getHasShowRewards(mAccount.id)
        binding.filterShowFinalizationsRewards.isChecked =
            session.getHasShowFinalizationRewards(mAccount.id)
        binding.filterShowFinalizationsRewards.isEnabled = binding.filterShowRewards.isChecked

        binding.filterShowRewards.setOnCheckedChangeListener { _, isChecked ->
            session.setHasShowRewards(mAccount.id, isChecked)
            if (!isChecked) {
                binding.filterShowFinalizationsRewards.isEnabled = false
                binding.filterShowFinalizationsRewards.isChecked = false
                session.setHasShowFinalizationRewards(mAccount.id, false)
            } else {
                binding.filterShowFinalizationsRewards.isEnabled = true
            }
        }
        binding.filterShowFinalizationsRewards.setOnCheckedChangeListener { _, isChecked ->
            session.setHasShowFinalizationRewards(mAccount.id, isChecked)
        }
    }

    // endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
    }

    private fun initViews() {
    }

    //endregion

    //region Control/UI
    // ************************************************************

    //endregion
}
