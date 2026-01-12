package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class HidingTokenDialog : BaseDialogFragment() {

    private val tokenName: String by lazy {
        requireNotNull(arguments?.getString(TOKEN_NAME)) {
            "No $TOKEN_NAME specified"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.cis_hide_token_title),
            description = getString(
                R.string.cis_hide_token_body,
                tokenName
            ),
            okButtonText = getString(R.string.cis_hide_token_confirm),
            cancelButtonText = getString(R.string.cis_hide_token_cancel)
        )

        listOf(binding.cancelButton, binding.closeButton).forEach {
            it.setOnClickListener {
                setFragmentResult(
                    ACTION_REQUEST,
                    getResultBundle(isHiding = false)
                )
                dismiss()
            }
        }

        binding.okButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                getResultBundle(isHiding = true)
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "hiding_token"
        const val ACTION_REQUEST = "hiding_action"
        private const val TOKEN_NAME = "token_name"
        private const val IS_HIDING = "is_hiding"

        fun newInstance(bundle: Bundle) = HidingTokenDialog().apply {
            arguments = bundle
        }

        fun getBundle(tokenName: String) = Bundle().apply {
            putString(TOKEN_NAME, tokenName)
        }

        private fun getResultBundle(isHiding: Boolean) = Bundle().apply {
            putBoolean(IS_HIDING, isHiding)
        }

        fun getResult(bundle: Bundle): Boolean = bundle.getBoolean(IS_HIDING, false)
    }
}