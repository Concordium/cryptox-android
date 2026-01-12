package com.concordium.wallet.ui.plt

import android.os.Bundle
import android.view.Gravity
import android.view.View
import com.concordium.wallet.R
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class PLTInfoDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.account_details_dialog_plt_title),
            description = getString(R.string.account_details_dialog_plt_description),
            okButtonText = getString(R.string.account_details_dialog_done)
        )

        binding.detailsTextView.gravity = Gravity.START
    }

    companion object {
        const val TAG = "PLTInfoDialog"
    }
}