package com.concordium.wallet.ui.bakerdelegation.dialog.baker

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.concordium.wallet.R
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class BakerNoticeDialog : BaseDialogFragment() {

    private val noticeMessage: String by lazy { arguments?.getString(NOTICE_MESSAGE) ?: "" }
    private val redirectToMainActivity: Boolean by lazy {
        arguments?.getBoolean(REDIRECT_TO_MAIN) ?: true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.baker_notice_title),
            description = noticeMessage,
            okButtonText = getString(R.string.baker_notice_ok)
        )

        binding.okButton.setOnClickListener {
            dismiss()
            if (redirectToMainActivity) {
                startActivity(
                    Intent(requireContext(), MainActivity::class.java).apply {
                        putExtra(MainActivity.EXTRA_GOTO_EARN, true)
                    }
                )
                requireActivity().finishAffinity()
            }
        }
    }

    companion object {
        const val TAG = "BakerNoticeDialog"
        private const val NOTICE_MESSAGE = "notice_message"
        private const val REDIRECT_TO_MAIN = "redirect_to_main"

        fun newInstance(bundle: Bundle) = BakerNoticeDialog().apply {
            arguments = bundle
        }

        fun setBundle(
            noticeMessage: String,
            redirectToMainActivity: Boolean = true
        ) = Bundle().apply {
            putString(NOTICE_MESSAGE, noticeMessage)
            putBoolean(REDIRECT_TO_MAIN, redirectToMainActivity)
        }
    }
}