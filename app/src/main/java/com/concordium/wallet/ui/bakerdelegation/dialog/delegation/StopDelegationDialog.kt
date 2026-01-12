package com.concordium.wallet.ui.bakerdelegation.dialog.delegation

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class StopDelegationDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.delegation_remove_subtitle1),
            description = getString(R.string.delegation_remove_description),
            okButtonText = getString(R.string.delegation_validation_continue_button),
            cancelButtonText = getString(R.string.delegation_validation_go_back_button)
        )

        binding.detailsTextView.gravity = Gravity.START

        listOf(binding.closeButton, binding.cancelButton).forEach {
            it.setOnClickListener {
                setFragmentResult(
                    ACTION_KEY,
                    setResultBundle(false)
                )
                dismiss()
            }
        }

        binding.okButton.setOnClickListener {
            setFragmentResult(
                ACTION_KEY,
                setResultBundle(true)
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "StopDelegationDialog"
        const val ACTION_KEY = "continue_to_remove"
        private const val CONTINUE = "continue"

        fun getResult(bundle: Bundle): Boolean = bundle.getBoolean(CONTINUE, false)

        private fun setResultBundle(isContinue: Boolean) = Bundle().apply {
            putBoolean(CONTINUE, isContinue)
        }
    }
}