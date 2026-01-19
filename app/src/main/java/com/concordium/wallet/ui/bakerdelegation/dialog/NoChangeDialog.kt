package com.concordium.wallet.ui.bakerdelegation.dialog

import android.os.Bundle
import android.view.View
import com.concordium.wallet.R
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class NoChangeDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.delegation_no_changes_title),
            description = getString(R.string.delegation_no_changes_message),
            okButtonText = getString(R.string.delegation_no_changes_ok)
        )
    }

    companion object {
        const val TAG = "NoChangeDialog"
    }
}