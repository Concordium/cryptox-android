package com.concordium.wallet.ui.account.earn

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.databinding.DialogEarnLegalDisclaimerBinding
import com.concordium.wallet.uicore.dialog.BaseGradientDialogFragment

class EarnLegalDisclaimerDialog : BaseGradientDialogFragment() {

    private lateinit var binding: DialogEarnLegalDisclaimerBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogEarnLegalDisclaimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.okButton.setOnClickListener { dismiss() }
    }

    companion object {
        const val TAG = "EarnLegalDisclaimerDialog"
    }
}