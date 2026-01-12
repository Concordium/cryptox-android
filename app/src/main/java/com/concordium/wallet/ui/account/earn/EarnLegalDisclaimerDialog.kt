package com.concordium.wallet.ui.account.earn

import android.os.Bundle
import android.text.util.Linkify
import android.view.Gravity
import android.view.View
import com.concordium.wallet.R
import com.concordium.wallet.uicore.dialog.BaseDialogFragment
import com.concordium.wallet.uicore.handleUrlClicks
import com.concordium.wallet.util.IntentUtil

class EarnLegalDisclaimerDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.earn_legal_disclaimer_title),
            description = getString(R.string.earn_legal_disclaimer_description),
            okButtonText = getString(R.string.earn_dialog_close)
        )
        binding.detailsTextView.gravity = Gravity.START

        Linkify.addLinks(binding.detailsTextView, Linkify.WEB_URLS)
        binding.detailsTextView.handleUrlClicks { url ->
            IntentUtil.openUrl(requireActivity(), url)
        }
    }

    companion object {
        const val TAG = "EarnLegalDisclaimerDialog"
    }
}