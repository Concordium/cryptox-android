package com.concordium.wallet.ui.bakerdelegation.dialog.delegation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class DelegationErrorDialog : BaseDialogFragment() {

    private val errorMessage: String by lazy { arguments?.getString(ERROR_MESSAGE)?: "" }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.delegation_register_delegation_failed_title),
            description = errorMessage,
            okButtonText = getString(R.string.delegation_register_delegation_failed_try_again),
            cancelButtonText = getString(R.string.delegation_register_delegation_failed_later)
        )

        binding.okButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                getResultBundle(tryAgain = true)
            )
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                getResultBundle(tryAgain = false)
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "DelegationErrorDialog"
        const val ACTION_REQUEST = "delegation_try_again_action"
        private const val ERROR_MESSAGE = "delegation_error_message"
        private const val TRY_AGAIN = "try_again"

        fun newInstance(bundle: Bundle) = DelegationErrorDialog().apply {
            arguments = bundle
        }

        fun setBundle(errorMessage: String) = Bundle().apply {
            putString(ERROR_MESSAGE, errorMessage)
        }

        fun getResult(bundle: Bundle): Boolean = bundle.getBoolean(TRY_AGAIN, false)

        private fun getResultBundle(tryAgain: Boolean) = Bundle().apply {
            putBoolean(TRY_AGAIN, tryAgain)
        }
    }
}