package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class MemoNoticeDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.transaction_memo_warning_title),
            description = getString(R.string.transaction_memo_warning_text),
            okButtonText = getString(R.string.transaction_memo_warning_ok),
            cancelButtonText = getString(R.string.transaction_memo_warning_dont_show),
        )

        listOf(binding.okButton, binding.closeButton).forEach {
            it.setOnClickListener {
                setFragmentResult(
                    ACTION_REQUEST,
                    getResultBundle(showAgain = true)
                )
                dismiss()
            }
        }

        binding.cancelButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                getResultBundle(showAgain = false)
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "memo_notice"
        const val ACTION_REQUEST = "memo_notice_action"
        private const val SHOW_AGAIN = "show_notice_again"

        fun getResult(bundle: Bundle) = bundle.getBoolean(SHOW_AGAIN, true)

        private fun getResultBundle(showAgain: Boolean) = Bundle().apply {
            putBoolean(SHOW_AGAIN, showAgain)
        }
    }
}