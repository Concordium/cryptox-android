package com.concordium.wallet.ui.multiwallet

import android.os.Bundle
import android.view.View
import com.concordium.wallet.R
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class FileWalletCreationLimitationDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.file_wallet_creation_limitation),
            description = getString(R.string.file_wallet_creation_limitation_explanation),
            okButtonText = getString(R.string.file_wallet_creation_limitation_okay)
        )
    }

    companion object {
        const val TAG = "file-wallet-creation-limitation"
    }
}
