package com.concordium.wallet.uicore.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogUnlockFeatureBinding

class UnlockFeatureDialog : AppCompatDialogFragment() {

    override fun getTheme(): Int = R.style.CCX_Dialog

    private lateinit var binding: DialogUnlockFeatureBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogUnlockFeatureBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        listOf(binding.closeButton, binding.goBackButton).forEach {
            it.setOnClickListener {
                dismiss()
            }
        }
    }

    companion object {
        const val TAG = "unlock_feature"
    }
}