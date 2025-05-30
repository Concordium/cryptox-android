package com.concordium.wallet.ui.seed.reveal.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.databinding.DialogDeleteBackupConfirmBinding
import com.concordium.wallet.uicore.dialog.BaseGradientDialogFragment

class DeleteBackupConfirmDialog : BaseGradientDialogFragment() {

    private lateinit var binding: DialogDeleteBackupConfirmBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogDeleteBackupConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listOf(binding.closeButton, binding.goBackButton).forEach {
            it.setOnClickListener {
                dismiss()
            }
        }

        binding.continueButton.setOnClickListener {
            setFragmentResult(
                DELETE_ACTION_REQUEST,
                setResultBundle(true)
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "DeleteBackupConfirmDialog"
        const val DELETE_ACTION_REQUEST = "delete_backup_action_request"
        private const val IS_DELETING = "is_deleting"

        fun setResultBundle(isDeleting: Boolean) = Bundle().apply {
            putBoolean(IS_DELETING, isDeleting)
        }

        fun getResult(bundle: Bundle): Boolean = bundle.getBoolean(IS_DELETING, false)
    }
}