package com.concordium.wallet.ui.more.notifications

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityNotificationsPreferencesBinding
import com.concordium.wallet.ui.base.BaseActivity

class NotificationsPreferencesActivity : BaseActivity(
    R.layout.activity_notifications_preferences,
    R.string.notifications_preferences_title,
) {
    private val binding: ActivityNotificationsPreferencesBinding by lazy {
        ActivityNotificationsPreferencesBinding.bind(findViewById(R.id.root_layout))
    }
    private val viewModel: NotificationsPreferencesViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViewModel()
        initViews()

        hideActionBarBack(isVisible = true)
    }

    private fun initViewModel() {
        viewModel.areCcdTxNotificationsEnabledLiveData.observe(
            this,
            binding.ccdTxSwitch::setChecked
        )
        viewModel.areCis2TxNotificationsEnabledLiveData.observe(
            this,
            binding.cis2TxSwitch::setChecked
        )
    }

    private fun initViews() = with(binding) {
        ccdTxTextView.setOnClickListener {
            ccdTxSwitch.callOnClick()
        }

        ccdTxSwitch.setOnClickListener {
            viewModel.onCcdTxClicked()
        }

        cis2TxTextView.setOnClickListener {
            ccdTxSwitch.callOnClick()
        }

        cis2TxSwitch.setOnClickListener {
            viewModel.onCis2TxClicked()
        }
    }
}
