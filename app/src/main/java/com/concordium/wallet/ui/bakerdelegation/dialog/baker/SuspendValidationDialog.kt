package com.concordium.wallet.ui.bakerdelegation.dialog.baker

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class SuspendValidationDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.baker_suspend_validation_dialog_title),
            description = getString(R.string.baker_suspend_validation_dialog_description),
            okButtonText = getString(R.string.baker_suspend_validation_dialog_button),
            cancelButtonText = getString(R.string.baker_suspend_validation_dialog_go_back_button),
        )
        binding.detailsTextView.gravity = Gravity.START

        listOf(binding.closeButton, binding.cancelButton).forEach {
            it.setOnClickListener {
                setFragmentResult(
                    ACTION_SUSPEND_CONTINUE,
                    setResultBundle(isContinue = false)
                )
                dismiss()
            }
        }

        binding.okButton.setOnClickListener {
            setFragmentResult(
                ACTION_SUSPEND_CONTINUE,
                setResultBundle(isContinue = true)
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "SuspendValidationDialog"
        const val ACTION_SUSPEND_CONTINUE = "action_suspend_continue"
        private const val CONTINUE = "continue"

        fun getResult(bundle: Bundle): Boolean = bundle.getBoolean(CONTINUE, false)

        private fun setResultBundle(isContinue: Boolean) = Bundle().apply {

            putBoolean(CONTINUE, isContinue)
        }
    }
}