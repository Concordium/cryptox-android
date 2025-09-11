package com.concordium.wallet.ui.account.accountdetails

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Schedule
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityAccountReleaseScheduleBinding
import com.concordium.wallet.ui.base.BaseActivity
import java.math.BigInteger

class AccountReleaseScheduleActivity : BaseActivity(
    R.layout.activity_account_release_schedule,
) {

    private val viewModel: AccountReleaseScheduleViewModel by viewModels()
    private val binding by lazy {
        ActivityAccountReleaseScheduleBinding.bind(findViewById(R.id.root_layout))
    }

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
    }

    //region Lifecycle
    // ************************************************************

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val account = intent.extras?.getSerializable(EXTRA_ACCOUNT) as Account
        viewModel.initialize(account)
        hideActionBarBack(isVisible = true)
        setActionBarTitle(getString(R.string.account_release_schedule_title))

        initHeader()
        initList()
    }

    // endregion

    //region Initialize
    // ************************************************************

    private fun initHeader() {
        binding.accountNameTextView.text = viewModel.account.getAccountName()
        binding.lockedAmountTextView.text =
            getString(
                R.string.amount,
                CurrencyUtil.formatGTU(
                    value = viewModel.account.releaseSchedule?.total ?: BigInteger.ZERO,
                )
            )
    }

    private fun initList() {
        val schedule: List<Schedule> =
            viewModel
                .account
                .releaseSchedule
                ?.schedule
                ?: emptyList()

        if (schedule.isEmpty()) {
            binding.noDataTextView.isVisible = true
            binding.scheduleRecyclerview.isVisible = false
            return
        }

        binding.noDataTextView.isVisible = false
        binding.scheduleRecyclerview.isVisible = true

        val adapter = AccountReleaseScheduleAdapter(
            clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager,
        )
        adapter.setData(schedule)
        binding.scheduleRecyclerview.adapter = adapter
    }

    //endregion
}
