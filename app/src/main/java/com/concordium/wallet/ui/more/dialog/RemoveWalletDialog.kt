package com.concordium.wallet.ui.more.dialog

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class RemoveWalletDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.settings_overview_erase_data_confirmation_title),
            description = getString(R.string.settings_overview_erase_wallet_confirmation_message),
            okButtonText = getString(R.string.settings_overview_erase_data_continue),
            cancelButtonText = getString(R.string.wallet_connect_clear_data_warning_cancel)
        )

        listOf(binding.cancelButton, binding.closeButton).forEach {
            it.setOnClickListener {
                setFragmentResult(
                    ACTION_REQUEST,
                    setResultBundle(false)
                )
                dismiss()
            }
        }

        binding.okButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                setResultBundle(true)
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "RemoveWalletDialog"
        const val ACTION_REQUEST = "remove_action"
        private const val IS_REMOVING = "is_removing"

        private fun setResultBundle(isRemoving: Boolean) = Bundle().apply {
            putBoolean(IS_REMOVING, isRemoving)
        }

        fun getResult(bundle: Bundle): Boolean = bundle.getBoolean(IS_REMOVING, false)
    }
}