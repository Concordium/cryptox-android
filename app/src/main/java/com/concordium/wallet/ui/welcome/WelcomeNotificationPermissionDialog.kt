package com.concordium.wallet.ui.welcome

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
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogWelcomeNotificationPermissionBinding
import com.concordium.wallet.util.Log

class WelcomeNotificationPermissionDialog : AppCompatDialogFragment() {
    override fun getTheme(): Int =
        R.style.CCX_Dialog

    private lateinit var binding: DialogWelcomeNotificationPermissionBinding

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
        binding = DialogWelcomeNotificationPermissionBinding.inflate(inflater, container, false)
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
    }

    private fun onNotificationPermissionResult(isGranted: Boolean) {
        Log.d(
            "received_result:" +
                    "\nisGranted=$isGranted"
        )

        dismiss()
        (requireActivity() as? Listener)
            ?.onNotificationPermissionDialogDismissed(isGranted)
    }

    interface Listener {
        fun onNotificationPermissionDialogDismissed(isGranted: Boolean)
    }

    companion object {
        const val TAG = "notification-permission"
    }
}
