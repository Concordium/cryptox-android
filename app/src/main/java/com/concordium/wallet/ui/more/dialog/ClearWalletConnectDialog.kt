package com.concordium.wallet.ui.more.dialog

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class ClearWalletConnectDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.wallet_connect_clear_data_warning_title),
            description = getString(R.string.wallet_connect_clear_data_warning_message),
            okButtonText = getString(R.string.wallet_connect_clear_data_warning_ok),
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
        const val TAG = "ClearWalletConnectDialog"
        const val ACTION_REQUEST = "clear_action"
        private const val IS_CLEARING = "is_clearing"

        private fun setResultBundle(isClearing: Boolean) = Bundle().apply {
            putBoolean(IS_CLEARING, isClearing)
        }

        fun getResult(bundle: Bundle): Boolean = bundle.getBoolean(IS_CLEARING, false)
    }
}