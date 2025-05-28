package com.concordium.wallet.ui.seed.reveal.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    }

    companion object {
        const val TAG = "DeleteBackupConfirmDialog"
    }
}