package com.concordium.wallet.ui.bakerdelegation.dialog.baker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogBakerNoticeDialogBinding
import com.concordium.wallet.ui.MainActivity

class BakerNoticeDialog : AppCompatDialogFragment() {

    override fun getTheme(): Int = R.style.CCX_Dialog

    private lateinit var binding: DialogBakerNoticeDialogBinding
    private val noticeMessage: String by lazy { arguments?.getString(NOTICE_MESSAGE)?: "" }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogBakerNoticeDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.detailsTextView.text = noticeMessage

        binding.okButton.setOnClickListener {
            dismiss()
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finishAffinity()
        }
    }

    companion object {
        const val TAG = "BakerNoticeDialog"
        private const val NOTICE_MESSAGE = "notice_message"

        fun newInstance(bundle: Bundle) = BakerNoticeDialog().apply {
            arguments = bundle
        }

        fun setBundle(noticeMessage: String) = Bundle().apply {
            putString(NOTICE_MESSAGE, noticeMessage)
        }
    }
}