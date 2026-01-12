package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class RawMetadataDialog : BaseDialogFragment() {

    private val metaDataString: String by lazy {
        requireNotNull(arguments?.getString(RAW_METADATA)) {
            "Metadata is empty"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.titleTextView.isVisible = false

        setViews(
            title = "",
            description = metaDataString,
            okButtonText = getString(R.string.cis_hide_token_cancel)
        )
        binding.detailsTextView.gravity = Gravity.START
    }

    companion object {
        const val TAG = "RawMetadataDialog"
        private const val RAW_METADATA = "RAW_METADATA"

        fun newInstance(bundle: Bundle) = RawMetadataDialog().apply {
            arguments = bundle
        }

        fun setBundle(rawMetadata: String) = Bundle().apply {
            putString(RAW_METADATA, rawMetadata)
        }
    }
}