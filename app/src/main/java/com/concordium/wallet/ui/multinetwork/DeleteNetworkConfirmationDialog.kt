package com.concordium.wallet.ui.multinetwork

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class DeleteNetworkConfirmationDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.delete_network),
            description = getString(
                R.string.template_delete_network_confirmation,
                requireArguments().getString(NETWORK_NAME)!!,
            ),
            okButtonText = getString(R.string.delete_network),
            cancelButtonText = getString(R.string.dialog_cancel)
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
        const val TAG = "DeleteNetworkConfirmationDialog"
        const val ACTION_REQUEST = "delete_confirm_action"
        private const val IS_CONFIRMED = "is_confirmed"
        private const val NETWORK_NAME = "network_name"

        private fun setResultBundle(isClearing: Boolean) = Bundle().apply {
            putBoolean(IS_CONFIRMED, isClearing)
        }

        fun setBundle(networkName: String) = Bundle().apply {
            putString(NETWORK_NAME, networkName)
        }

        fun newInstance(bundle: Bundle) = DeleteNetworkConfirmationDialog().apply {
            arguments = bundle
        }

        fun getResult(bundle: Bundle): Boolean =
            bundle.getBoolean(IS_CONFIRMED, false)
    }
}
