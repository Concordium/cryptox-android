package com.concordium.wallet.ui.more.notifications

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityNotificationsPreferencesBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.Log
import kotlinx.coroutines.launch

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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.areCcdTxNotificationsEnabledFlow.collect {
                        binding.ccdTxSwitch.isChecked = it
                    }
                }
                launch {
                    viewModel.isCcdSwitchEnabledFlow.collect {
                        binding.ccdTxSwitch.isEnabled = it
                        binding.progress.progressBar.isVisible = it.not()
                    }
                }
                launch {
                    viewModel.areCis2TxNotificationsEnabledFlow.collect {
                        binding.cis2TxSwitch.isChecked = it
                    }
                }
                launch {
                    viewModel.isCis2SwitchEnabledFlow.collect {
                        binding.cis2TxSwitch.isEnabled = it
                        binding.progress.progressBar.isVisible = it.not()
                    }
                }
                launch {
                    viewModel.requestNotificationPermissionFlow.collect {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Unit)
                        } else {
                            Log.w("requesting_permission_on_unsupported_version")
                        }
                    }
                }
            }
        }
    }

    private fun initViews() = with(binding) {
        ccdTxTextView.setOnClickListener {
            ccdTxSwitch.performClick()
        }

        ccdTxSwitch.setOnClickListener {
            viewModel.onCcdTxClicked()
        }

        cis2TxTextView.setOnClickListener {
            cis2TxSwitch.performClick()
        }

        cis2TxSwitch.setOnClickListener {
            viewModel.onCis2TxClicked()
        }
    }

    private fun onNotificationPermissionResult(isGranted: Boolean) {
        Log.d(
            "received_result:" +
                    "\nisGranted=$isGranted"
        )
    }
}
