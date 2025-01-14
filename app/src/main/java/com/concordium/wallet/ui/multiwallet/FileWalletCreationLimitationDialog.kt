package com.concordium.wallet.ui.multiwallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogFileWalletCreationLimitationBinding

class FileWalletCreationLimitationDialog : AppCompatDialogFragment() {
    override fun getTheme(): Int =
        R.style.CCX_Dialog

    private lateinit var binding: DialogFileWalletCreationLimitationBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogFileWalletCreationLimitationBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        listOf(binding.gotItButton, binding.closeButton).forEach {
            it.setOnClickListener {
                dismiss()
            }
        }
    }

    companion object {
        const val TAG = "file-wallet-creation-limitation"
    }
}
