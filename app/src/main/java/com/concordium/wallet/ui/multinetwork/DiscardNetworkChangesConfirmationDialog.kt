package com.concordium.wallet.ui.multinetwork

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class DiscardNetworkChangesConfirmationDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.discard_network_changes),
            description = getString(R.string.discard_network_changes_confirmation),
            okButtonText = getString(R.string.discard),
            cancelButtonText = getString(R.string.keep_editing)
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
        const val TAG = "DiscardNetworkChangesConfirmationDialog"
        const val ACTION_REQUEST = "discard_confirm_action"
        private const val IS_CONFIRMED = "is_confirmed"

        private fun setResultBundle(isClearing: Boolean) = Bundle().apply {
            putBoolean(IS_CONFIRMED, isClearing)
        }

        fun newInstance() = DiscardNetworkChangesConfirmationDialog()

        fun getResult(bundle: Bundle): Boolean =
            bundle.getBoolean(IS_CONFIRMED, false)
    }
}
