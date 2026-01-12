package com.concordium.wallet.ui.identity.identityconfirmed

import android.os.Bundle
import android.view.View
import com.concordium.wallet.R
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class IdentityRequestNoticeDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(requireNotNull(arguments?.getInt(TITLE_ID_EXTRA))),
            description = getString(requireNotNull(arguments?.getInt(DETAILS_ID_EXTRA))),
            okButtonText = getString(R.string.identity_confirmed_notice_got_it)
        )
    }

    companion object {
        const val TAG = "confirmed-id-notice"
        private const val IMAGE_ID_EXTRA = "image_id"
        private const val TITLE_ID_EXTRA = "title_id"
        private const val DETAILS_ID_EXTRA = "details_id"

        fun submitted() = IdentityRequestNoticeDialog().apply {
            arguments = Bundle().apply {
                putInt(IMAGE_ID_EXTRA, R.drawable.ccx_submitted_verification)
                putInt(TITLE_ID_EXTRA, R.string.identity_confirmed_notice_submitted_title)
                putInt(DETAILS_ID_EXTRA, R.string.identity_confirmed_notice_submitted_details)
            }
        }

        fun approved() = IdentityRequestNoticeDialog().apply {
            arguments = Bundle().apply {
                putInt(IMAGE_ID_EXTRA, R.drawable.ccx_successful_verification)
                putInt(TITLE_ID_EXTRA, R.string.identity_confirmed_notice_approved_title)
                putInt(DETAILS_ID_EXTRA, R.string.identity_confirmed_notice_approved_details)
            }
        }
    }
}
