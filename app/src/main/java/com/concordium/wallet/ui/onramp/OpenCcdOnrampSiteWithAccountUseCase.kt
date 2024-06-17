package com.concordium.wallet.ui.onramp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.concordium.wallet.R

class OpenCcdOnrampSiteWithAccountUseCase(
    val site: CcdOnrampSite,
    val accountAddress: String,
    val onAccountAddressCopied: () -> Unit,
    val context: Context,
) {
    operator fun invoke() {
        val clipboardManager: ClipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(
            context.getString(R.string.account_details_address),
            accountAddress,
        )
        onAccountAddressCopied()
        clipboardManager.setPrimaryClip(clipData)

        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(site.url))
        context.startActivity(Intent.createChooser(browserIntent, site.name))
    }
}
