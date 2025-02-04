package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogRawMetadataBinding

class RawMetadataDialog : AppCompatDialogFragment() {

    override fun getTheme(): Int = R.style.CCX_Dialog

    private lateinit var binding: DialogRawMetadataBinding

    private val metaDataString: String by lazy {
        requireNotNull(arguments?.getString(RAW_METADATA)) {
            "Metadata is empty"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogRawMetadataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.detailsTextView.text = metaDataString

        listOf(binding.denyButton, binding.closeButton).forEach {
            it.setOnClickListener { dismiss() }
        }
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