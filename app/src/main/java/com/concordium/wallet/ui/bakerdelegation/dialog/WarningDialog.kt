package com.concordium.wallet.ui.bakerdelegation.dialog

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class WarningDialog : BaseDialogFragment() {

    private val title: String by lazy { arguments?.getString(TITLE) ?: "" }
    private val description: String by lazy { arguments?.getString(DESCRIPTION) ?: "" }
    private val confirmButtonText: String by lazy { arguments?.getString(CONFIRM_BUTTON) ?: "" }
    private val denyButtonText: String by lazy { arguments?.getString(DENY_BUTTON) ?: "" }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = title,
            description = description,
            okButtonText = confirmButtonText,
            cancelButtonText = denyButtonText
        )

        binding.okButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                getResultBundle(isContinue = true)
            )
            dismiss()
        }

        listOf(binding.cancelButton, binding.closeButton).forEach {
            it.setOnClickListener {
                setFragmentResult(
                    ACTION_REQUEST,
                    getResultBundle(isContinue = false)
                )
                dismiss()
            }
        }
    }

    companion object {
        const val TAG = "DelegationWarningDialog"
        const val ACTION_REQUEST = "continue_action"
        private const val CONTINUE = "continue"
        private const val TITLE = "title"
        private const val DESCRIPTION = "description"
        private const val CONFIRM_BUTTON = "confirm_button"
        private const val DENY_BUTTON = "deny_button"

        fun newInstance(bundle: Bundle) = WarningDialog().apply {
            arguments = bundle
        }

        fun setBundle(
            title: String,
            description: String,
            confirmButton: String,
            denyButton: String
        ) = Bundle().apply {
            putString(TITLE, title)
            putString(DESCRIPTION, description)
            putString(CONFIRM_BUTTON, confirmButton)
            putString(DENY_BUTTON, denyButton)
        }

        fun getResult(bundle: Bundle): Boolean = bundle.getBoolean(CONTINUE, false)

        private fun getResultBundle(isContinue: Boolean) = Bundle().apply {
            putBoolean(CONTINUE, isContinue)
        }
    }
}