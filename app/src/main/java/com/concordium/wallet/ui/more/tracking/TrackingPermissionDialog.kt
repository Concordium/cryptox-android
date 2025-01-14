package com.concordium.wallet.ui.more.tracking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import com.concordium.wallet.R
import com.concordium.wallet.data.preferences.AppTrackingPreferences
import com.concordium.wallet.databinding.DialogTrackingPermissionBinding

class TrackingPermissionDialog : AppCompatDialogFragment() {
    override fun getTheme(): Int =
        R.style.CCX_Dialog

    private lateinit var binding: DialogTrackingPermissionBinding

    private val appTrackingPreferences: AppTrackingPreferences by lazy {
        AppTrackingPreferences(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogTrackingPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.closeButton.setOnClickListener {
            dismiss()
        }

        binding.denyButton.setOnClickListener {
            appTrackingPreferences.isTrackingEnabled = false
            appTrackingPreferences.hasDecidedOnPermission = true
            dismiss()
        }

        binding.allowButton.setOnClickListener {
            appTrackingPreferences.isTrackingEnabled = true
            appTrackingPreferences.hasDecidedOnPermission = true
            dismiss()
        }
    }

    companion object {
        const val TAG = "tracking-permission"
    }
}
