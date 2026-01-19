package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class NewlyReceivedTokenNoticeDialog : BaseDialogFragment() {

    private val tokenName: String by lazy {
        requireNotNull(arguments?.getString(TOKEN_NAME_KEY)) {
            "No $TOKEN_NAME_KEY specified"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.cis_newly_received_notice_title),
            description = getString(
                R.string.template_cis_newly_received_notice_message,
                tokenName
            ),
            okButtonText = getString(R.string.cis_newly_received_notice_keep_token),
            cancelButtonText = getString(R.string.cis_newly_received_notice_remove_token),
        )

        binding.okButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                getResultBundle(
                    isKeeping = true,
                )
            )
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                getResultBundle(
                    isKeeping = false,
                )
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "activate-account"
        const val ACTION_REQUEST = "token-action"
        private const val IS_KEEPING_KEY = "is_keeping"
        private const val TOKEN_NAME_KEY = "token_name"

        fun newInstance(bundle: Bundle) = NewlyReceivedTokenNoticeDialog().apply {
            arguments = bundle
        }

        fun getBundle(tokenName: String) = Bundle().apply {
            putString(TOKEN_NAME_KEY, tokenName)
        }

        private fun getResultBundle(isKeeping: Boolean) = Bundle().apply {
            putBoolean(IS_KEEPING_KEY, isKeeping)
        }

        /**
         * @return **true** if keeping the token.
         */
        fun getResult(bundle: Bundle): Boolean =
            bundle.getBoolean(IS_KEEPING_KEY, true)
    }
}
