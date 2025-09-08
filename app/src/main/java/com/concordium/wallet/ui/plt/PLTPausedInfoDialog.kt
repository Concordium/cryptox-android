package com.concordium.wallet.ui.plt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.databinding.DialogPltPausedInfoBinding
import com.concordium.wallet.uicore.dialog.BaseGradientDialogFragment

class PLTPausedInfoDialog : BaseGradientDialogFragment() {

    private lateinit var binding: DialogPltPausedInfoBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogPltPausedInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.doneButton.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        const val TAG = "PLTPausedInfoDialog"
    }
}