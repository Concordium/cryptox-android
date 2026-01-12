package com.concordium.wallet.ui.more.notifications

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.notifications.UpdateNotificationsSubscriptionUseCase
import com.concordium.wallet.uicore.dialog.BaseDialogFragment
import com.concordium.wallet.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class NotificationsPermissionDialog : BaseDialogFragment() {

    private val walletNotificationsPreferences =
        App.appCore.session.walletStorage.notificationsPreferences

    private val updateNotificationsSubscriptionUseCase by lazy(::UpdateNotificationsSubscriptionUseCase)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val notificationPermissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
            Manifest.permission.POST_NOTIFICATIONS,
            this::onNotificationPermissionResult
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setViews(
            title = getString(R.string.notifications_permission_title),
            description = getString(R.string.notifications_permission_details),
            okButtonText = getString(R.string.notifications_permission_allow),
            cancelButtonText = getString(R.string.notifications_permission_deny),
            iconResId = R.drawable.mw24_ic_notifications
        )

        listOf(binding.cancelButton, binding.closeButton).forEach {
            it.setOnClickListener {
                onNotificationPermissionResult(isGranted = false)
            }
        }

        binding.okButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Log.d(
                    "launching_system_request"
                )

                notificationPermissionsLauncher.launch(Unit)
            } else {
                Log.d(
                    "system_request_not_needed"
                )

                onNotificationPermissionResult(isGranted = true)
            }
        }

        // Track showing the dialog once it is visible to the user.
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                delay(500)
                walletNotificationsPreferences.hasEverShownPermissionDialog = true
            }
        }
    }

    private fun onNotificationPermissionResult(isGranted: Boolean) {
        Log.d(
            "received_result:" +
                    "\nisGranted=$isGranted"
        )

        walletNotificationsPreferences.enableAll(areNotificationsEnabled = isGranted)

        @OptIn(DelicateCoroutinesApi::class)
        if (isGranted) {
            GlobalScope.launch {
                withTimeout(10000) {
                    updateNotificationsSubscriptionUseCase()
                }
            }
        }

        dismiss()
    }

    companion object {
        const val TAG = "notification-permission"
    }
}
