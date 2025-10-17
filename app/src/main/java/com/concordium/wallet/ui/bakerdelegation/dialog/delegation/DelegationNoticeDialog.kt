package com.concordium.wallet.ui.bakerdelegation.dialog.delegation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.databinding.DialogDelegationNoticeBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.uicore.dialog.BaseGradientDialogFragment

class DelegationNoticeDialog : BaseGradientDialogFragment() {

    private lateinit var binding: DialogDelegationNoticeBinding
    private val noticeMessage: String by lazy { arguments?.getString(NOTICE_MESSAGE) ?: "" }
    private val showReviewDialog: Boolean by lazy {
        arguments?.getBoolean(SHOW_REVIEW_DIALOG) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogDelegationNoticeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.detailsTextView.text = noticeMessage

        binding.okButton.setOnClickListener {
            dismiss()
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                if (showReviewDialog) {
                    putExtra(MainActivity.EXTRA_SHOW_REVIEW_POPUP, true)
                }
            }
            startActivity(intent)
            requireActivity().finishAffinity()
        }
    }

    companion object {
        const val TAG = "DelegationNoticeDialog"
        private const val NOTICE_MESSAGE = "notice_message"
        private const val SHOW_REVIEW_DIALOG = "show_review_dialog"

        fun newInstance(bundle: Bundle) = DelegationNoticeDialog().apply {
            arguments = bundle
        }

        fun setBundle(noticeMessage: String, showReviewDialog: Boolean = false) = Bundle().apply {
            putString(NOTICE_MESSAGE, noticeMessage)
            putBoolean(SHOW_REVIEW_DIALOG, showReviewDialog)
        }
    }
}