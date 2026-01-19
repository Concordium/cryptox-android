package com.concordium.wallet.uicore.dialog

import android.os.Bundle
import android.view.View
import com.concordium.wallet.App
import com.concordium.wallet.R

class UnlockFeatureDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.unlock_feature_title),
            description = getString(R.string.unlock_feature_details),
            okButtonText = getString(R.string.unlock_feature_go_back_button),
            iconResId = R.drawable.mw24_ic_unlock_feature
        )

        App.appCore.tracker.homeUnlockFeatureDialog()
    }

    companion object {
        const val TAG = "unlock_feature"
    }
}