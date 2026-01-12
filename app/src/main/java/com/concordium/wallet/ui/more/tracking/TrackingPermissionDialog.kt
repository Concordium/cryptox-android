package com.concordium.wallet.ui.more.tracking

import android.os.Bundle
import android.view.View
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class TrackingPermissionDialog : BaseDialogFragment() {

    private val appTrackingPreferences = App.appCore.appTrackingPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.tracking_permission_title),
            description = getString(R.string.tracking_permission_message),
            okButtonText = getString(R.string.tracking_permission_allow),
            cancelButtonText = getString(R.string.tracking_permission_deny)
        )

        binding.cancelButton.setOnClickListener {
            appTrackingPreferences.isTrackingEnabled = false
            appTrackingPreferences.hasDecidedOnPermission = true
            dismiss()
        }

        binding.okButton.setOnClickListener {
            appTrackingPreferences.isTrackingEnabled = true
            appTrackingPreferences.hasDecidedOnPermission = true
            dismiss()
        }
    }

    companion object {
        const val TAG = "tracking-permission"
    }
}
