package com.concordium.wallet.ui.bakerdelegation.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogNoChangeBinding

class NoChangeDialog : AppCompatDialogFragment() {

    override fun getTheme(): Int = R.style.CCX_Dialog

    private lateinit var binding: DialogNoChangeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogNoChangeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.okButton.setOnClickListener { dismiss() }
    }

    companion object {
        const val TAG = "NoChangeDialog"
    }
}