package com.concordium.wallet.ui.more.notifications

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityNotificationsPreferencesBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.Log

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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
            Manifest.permission.POST_NOTIFICATIONS,
            this::onNotificationPermissionResult
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViewModel()
        initViews()

        hideActionBarBack(isVisible = true)
    }

    private fun initViewModel() {
        viewModel.areCcdTxNotificationsEnabledFlow.collectWhenStarted(this) {
            binding.ccdTxSwitch.isChecked = it
        }
        viewModel.isCcdSwitchEnabledFlow.collectWhenStarted(this) {
            binding.apply {
                ccdTxSwitch.isEnabled = it
                ccdTxTextView.isClickable = it
                progress.progressBar.isVisible = it.not()
            }
        }
        viewModel.areCis2TxNotificationsEnabledFlow.collectWhenStarted(this) {
            binding.cis2TxSwitch.isChecked = it
        }
        viewModel.isCis2SwitchEnabledFlow.collectWhenStarted(this) {
            binding.apply {
                cis2TxSwitch.isEnabled = it
                cis2TxTextView.isClickable = it
                progress.progressBar.isVisible = it.not()
            }
        }
        viewModel.arePltTxNotificationsEnabledFlow.collectWhenStarted(this) {
            binding.pltTxSwitch.isChecked = it
        }
        viewModel.isPltSwitchEnabledFlow.collectWhenStarted(this) {
            binding.apply {
                pltTxSwitch.isEnabled = it
                pltTxTextView.isClickable = it
                progress.progressBar.isVisible = it.not()
            }
        }
        viewModel.requestNotificationPermissionFlow.collectWhenStarted(this) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Unit)
            } else {
                Log.w("requesting_permission_on_unsupported_version")
            }
        }
    }

    private fun initViews() = with(binding) {
        ccdTxTextView.setOnClickListener {
            ccdTxSwitch.performClick()
        }

        ccdTxSwitch.setOnClickListener {
            ccdTxSwitch.isChecked = !ccdTxSwitch.isChecked
            viewModel.onTxClicked(NotificationsPreferencesViewModel.TxType.CCD)
        }

        cis2TxTextView.setOnClickListener {
            cis2TxSwitch.performClick()
        }

        cis2TxSwitch.setOnClickListener {
            cis2TxSwitch.isChecked = !cis2TxSwitch.isChecked
            viewModel.onTxClicked(NotificationsPreferencesViewModel.TxType.CIS2)
        }

        pltTxTextView.setOnClickListener {
            pltTxSwitch.performClick()
        }

        pltTxSwitch.setOnClickListener {
            pltTxSwitch.isChecked = !pltTxSwitch.isChecked
            viewModel.onTxClicked(NotificationsPreferencesViewModel.TxType.PLT)
        }
    }

    private fun onNotificationPermissionResult(isGranted: Boolean) {
        Log.d(
            "received_result:" +
                    "\nisGranted=$isGranted"
        )
    }
}
