package com.concordium.wallet.ui.recipient.recipientlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ConfirmationBottomsheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DeleteRecipientBottomSheet() : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.CCX_BottomSheetDialog

    private lateinit var binding: ConfirmationBottomsheetBinding
    private val recipientName: String by lazy {
        arguments?.getString(RECIPIENT_NAME) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ConfirmationBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.description.text = requireActivity().getString(
            R.string.confirmation_bottom_sheet_description,
            recipientName
        )

        binding.btnCancel.setOnClickListener {
            dismiss()
            setFragmentResult(
                ACTION_DELETE,
                setResultBundle(false)
            )
        }

        binding.btnDelete.setOnClickListener {
            dismiss()
            setFragmentResult(
                ACTION_DELETE,
                setResultBundle(true)
            )
        }
    }

    companion object {
        const val TAG = "DeleteRecipientBottomSheet"
        const val ACTION_DELETE = "action_delete"
        private const val IS_DELETING = "is_deleting"
        private const val RECIPIENT_NAME = "recipient_name"

        fun newInstance(bundle: Bundle) = DeleteRecipientBottomSheet().apply {
            arguments = bundle
        }

        fun setBundle(recipientName: String) = Bundle().apply {
            putString(RECIPIENT_NAME, recipientName)
        }

        fun getResult(bundle: Bundle): Boolean = bundle.getBoolean(IS_DELETING, false)

        private fun setResultBundle(isDeleting: Boolean) = Bundle().apply {
            putBoolean(IS_DELETING, isDeleting)
        }
    }
}
