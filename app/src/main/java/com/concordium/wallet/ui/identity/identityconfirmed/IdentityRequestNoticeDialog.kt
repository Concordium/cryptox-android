package com.concordium.wallet.ui.identity.identityconfirmed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogIdentityRequestNoticeBinding

class IdentityRequestNoticeDialog : AppCompatDialogFragment() {
    override fun getTheme(): Int =
        R.style.CCX_Dialog

    private lateinit var binding: DialogIdentityRequestNoticeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogIdentityRequestNoticeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.imageView.setImageResource(requireNotNull(arguments?.getInt(IMAGE_ID_EXTRA)))
        binding.titleTextView.setText(requireNotNull(arguments?.getInt(TITLE_ID_EXTRA)))
        binding.detailsTextView.setText(requireNotNull(arguments?.getInt(DETAILS_ID_EXTRA)))

        listOf(binding.closeButton, binding.gotItButton).forEach {
            it.setOnClickListener {
                dismiss()
            }
        }
    }

    companion object {
        const val TAG = "confirmed-id-notice"
        private const val IMAGE_ID_EXTRA = "image_id"
        private const val TITLE_ID_EXTRA = "title_id"
        private const val DETAILS_ID_EXTRA = "details_id"

        fun submitted() = IdentityRequestNoticeDialog().apply {
            arguments=Bundle().apply {
                putInt(IMAGE_ID_EXTRA, R.drawable.ccx_submitted_verification)
                putInt(TITLE_ID_EXTRA, R.string.identity_confirmed_notice_submitted_title)
                putInt(DETAILS_ID_EXTRA, R.string.identity_confirmed_notice_submitted_details)
            }
        }

        fun approved() = IdentityRequestNoticeDialog().apply {
            arguments=Bundle().apply {
                putInt(IMAGE_ID_EXTRA, R.drawable.ccx_successful_verification)
                putInt(TITLE_ID_EXTRA, R.string.identity_confirmed_notice_approved_title)
                putInt(DETAILS_ID_EXTRA, R.string.identity_confirmed_notice_approved_details)
            }
        }
    }
}
