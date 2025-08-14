package com.concordium.wallet.ui.onramp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.databinding.DialogOnrampDisclaimerBinding
import com.concordium.wallet.uicore.dialog.BaseGradientDialogFragment

class CcdOnrampDisclaimerDialog : BaseGradientDialogFragment() {

    private lateinit var binding: DialogOnrampDisclaimerBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogOnrampDisclaimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        const val TAG = "CcdOnrampDisclaimerDialog"
    }
}