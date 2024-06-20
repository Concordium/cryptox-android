package com.concordium.wallet.ui.more.notifications

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.lifecycleScope
import com.concordium.wallet.R
import com.concordium.wallet.data.preferences.NotificationsPreferences
import com.concordium.wallet.databinding.DialogNotificationsPermissionBinding
import com.concordium.wallet.util.Log
import kotlinx.coroutines.delay

class NotificationsPermissionDialog : AppCompatDialogFragment() {
    override fun getTheme(): Int =
        R.style.CCX_Dialog

    private lateinit var binding: DialogNotificationsPermissionBinding

    private val notificationsPreferences: NotificationsPreferences by lazy {
        NotificationsPreferences(requireContext())
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val notificationPermissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
            Manifest.permission.POST_NOTIFICATIONS,
            this::onNotificationPermissionResult
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogNotificationsPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        listOf(binding.denyButton, binding.closeButton).forEach {
            it.setOnClickListener {
                onNotificationPermissionResult(isGranted = false)
            }
        }

        binding.allowButton.setOnClickListener {
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
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            delay(500)
            notificationsPreferences.hasEverShownPermissionDialog = true
        }
    }

    private fun onNotificationPermissionResult(isGranted: Boolean) {
        Log.d(
            "received_result:" +
                    "\nisGranted=$isGranted"
        )

        notificationsPreferences.enableAll(areNotificationsEnabled = isGranted)

        dismiss()
    }

    companion object {
        const val TAG = "notification-permission"
    }
}
