package com.concordium.wallet.ui.account.accountdetails

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.AccountReleaseScheduleItemBinding
import com.concordium.wallet.databinding.AccountReleaseScheduleTransactionItemBinding
import com.concordium.wallet.databinding.ActivityAccountReleaseScheduleBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.toBigInteger
import java.math.BigInteger
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class AccountReleaseScheduleActivity : BaseActivity(
    R.layout.activity_account_release_schedule,
) {

    private lateinit var viewModel: AccountReleaseScheduleViewModel
    private val binding by lazy {
        ActivityAccountReleaseScheduleBinding.bind(findViewById(R.id.root_layout))
    }

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
        const val EXTRA_SHIELDED = "EXTRA_SHIELDED"
    }

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val account = intent.extras!!.getSerializable(EXTRA_ACCOUNT) as Account
        val isShielded = intent.extras!!.getBoolean(EXTRA_SHIELDED)
        initializeViewModel()
        viewModel.initialize(account, isShielded)
        hideActionBarBack(isVisible = true)
        initViews()
    }

    override fun onResume() {
        super.onResume()
        viewModel.populateScheduledReleaseList()
    }

    // endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AccountReleaseScheduleViewModel::class.java]

        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
        viewModel.finishLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                finish()
            }
        })
        viewModel.scheduledReleasesLiveData.observe(this) { list ->

            binding.accountReleaseScheduleLockedAmount.text = CurrencyUtil.formatGTU(
                viewModel.account.finalizedAccountReleaseSchedule?.total ?: BigInteger.ZERO,
                true
            )

            binding.accountReleaseScheduleList.removeAllViews()
            val dateFormat = DateFormat.getDateTimeInstance(
                DateFormat.SHORT,
                DateFormat.SHORT,
                Locale.getDefault()
            )
            list.forEach { release ->
                val view = AccountReleaseScheduleItemBinding.inflate(
                    LayoutInflater.from(this)
                )
                view.date.text = dateFormat.format(Date(release.timestamp))

                release.transactions.forEach { transaction ->
                    val viewTransaction = AccountReleaseScheduleTransactionItemBinding.inflate(
                        LayoutInflater.from(this)
                    )
                    viewTransaction.identifier.text = transaction.subSequence(0, 8)
                    view.identifierContainer.addView(viewTransaction.root)
                    viewTransaction.copy.tag = transaction
                    viewTransaction.copy.setOnClickListener {
                        val clipboard: ClipboardManager =
                            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(
                            getString(R.string.account_release_schedule_copy_title),
                            it.tag.toString()
                        )
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(
                            this,
                            getString(R.string.account_release_schedule_copied),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                view.amount.text = CurrencyUtil.formatGTU(release.amount.toBigInteger(), true)
                binding.accountReleaseScheduleList.addView(view.root)
            }

            binding.noDataTextView.isVisible = list.isEmpty()
        }
    }

    private fun initViews() {
        setActionBarTitle(
            getString(
                R.string.account_release_schedule_title,
                viewModel.account.getAccountName()
            )
        )
        showWaiting(false)
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }
    //endregion
}
