package com.concordium.wallet.ui.onramp

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogOnrampDisclaimerBinding
import com.concordium.wallet.uicore.dialog.BaseGradientDialogFragment

class CcdOnrampDisclaimerDialog : BaseGradientDialogFragment() {

    private lateinit var binding: DialogOnrampDisclaimerBinding

    private val isDisclaimerAccepted: Boolean by lazy {
        arguments?.getBoolean(IS_DISCLAIMER_ACCEPTED, false) ?: false
    }
    private val showSiteIcon: Boolean by lazy {
        arguments?.getBoolean(SHOW_SITE_ICON, false) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogOnrampDisclaimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isDisclaimerAccepted) {
            binding.acceptLayout.isVisible = false
            binding.closeButton.isVisible = true
        } else {
            binding.acceptLayout.isVisible = true
            binding.closeButton.isVisible = false
            if (showSiteIcon) {
                binding.proceedButton.setIconEndDrawable(
                    AppCompatResources.getDrawable(
                        requireContext(), R.drawable.mw24_ic_external_link
                    )
                )
            }
        }

        listOf(binding.closeButton, binding.cancelButton).forEach {
            it.setOnClickListener {
                setFragmentResult(
                    ACTION_REQUEST,
                    setResultBundle(isAccepted = false)
                )
                dismiss()
            }
        }

        binding.proceedButton.setClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                setResultBundle(isAccepted = true)
            )
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!isDisclaimerAccepted) {
            setFragmentResult(
                ACTION_REQUEST,
                setResultBundle(isAccepted = false)
            )
        }
    }

    companion object {
        const val TAG = "CcdOnrampDisclaimerDialog"
        const val ACTION_REQUEST = "CcdOnrampDisclaimerDialog_request"
        private const val IS_DISCLAIMER_ACCEPTED = "is_disclaimer_accepted"
        private const val SHOW_SITE_ICON = "show_site_icon"
        private const val ACCEPT_DISCLAIMER = "accept_disclaimer"

        fun newInstance(bundle: Bundle) = CcdOnrampDisclaimerDialog().apply {
            arguments = bundle
        }

        fun setBundle(isDisclaimerAccepted: Boolean, showSiteIcon: Boolean) = Bundle().apply {
            putBoolean(IS_DISCLAIMER_ACCEPTED, isDisclaimerAccepted)
            putBoolean(SHOW_SITE_ICON, showSiteIcon)
        }

        fun getResult(bundle: Bundle): Boolean = bundle.getBoolean(ACCEPT_DISCLAIMER, false)

        private fun setResultBundle(isAccepted: Boolean) = Bundle().apply {
            putBoolean(ACCEPT_DISCLAIMER, isAccepted)
        }
    }
}