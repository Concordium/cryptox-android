package com.concordium.wallet.ui.bakerdelegation.dialog

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.concordium.wallet.R
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class NotEnoughFundsForFeeDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.delegation_remove_not_enough_funds_title),
            description = getString(R.string.delegation_remove_not_enough_funds_message),
            okButtonText = getString(R.string.delegation_remove_not_enough_funds_ok)
        )

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
