package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.databinding.DialogCis2InfoBinding
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class CIS2InfoDialog : BaseDialogFragment() {

    private lateinit var binding: DialogCis2InfoBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogCis2InfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.doneButton.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        const val TAG = "CIS2InfoDialog"
    }
}