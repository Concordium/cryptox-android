package com.concordium.wallet.ui.more.tracking

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityTrackingPreferencesBinding
import com.concordium.wallet.ui.base.BaseActivity

class TrackingPreferencesActivity : BaseActivity(
    R.layout.activity_tracking_preferences,
    R.string.tracking_preferences_title,
) {
    private val binding: ActivityTrackingPreferencesBinding by lazy {
        ActivityTrackingPreferencesBinding.bind(findViewById(R.id.root_layout))
    }
    private val viewModel: TrackingPreferencesViewModel by lazy {
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
        viewModel.isTrackingEnabledLiveData.observe(this, binding.allowSwitch::setChecked)
    }

    private fun initViews() = with(binding) {
        allowTextView.setOnClickListener {
            allowSwitch.callOnClick()
        }

        allowSwitch.setOnClickListener {
            viewModel.onAllowClicked()
        }
    }
}
