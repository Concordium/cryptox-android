package com.concordium.wallet.ui.bakerdelegation.dialog

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogNotEnoughFundsForFeeBinding
import com.concordium.wallet.ui.MainActivity

class NotEnoughFundsForFeeDialog : AppCompatDialogFragment() {

    override fun getTheme(): Int = R.style.CCX_Dialog

    private lateinit var binding: DialogNotEnoughFundsForFeeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogNotEnoughFundsForFeeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.okButton.setOnClickListener {
            dismiss()
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finishAffinity()
        }
    }

    companion object {
        const val TAG = "NotEnoughFundsForFeeDialog"
    }
}
