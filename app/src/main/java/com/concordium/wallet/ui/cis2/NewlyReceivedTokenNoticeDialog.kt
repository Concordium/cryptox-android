package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogNewlyReceivedTokenNoticeBinding

class NewlyReceivedTokenNoticeDialog : AppCompatDialogFragment() {
    override fun getTheme(): Int =
        R.style.CCX_Dialog

    private lateinit var binding: DialogNewlyReceivedTokenNoticeBinding

    private val tokenName: String by lazy {
        requireNotNull(arguments?.getString(TOKEN_NAME_KEY)) {
            "No $TOKEN_NAME_KEY specified"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogNewlyReceivedTokenNoticeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.detailsTextView.text = getString(
            R.string.template_cis_newly_received_notice_message,
            tokenName
        )

        binding.keepButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                getResultBundle(
                    isKeeping = true,
                )
            )
            dismiss()
        }

        binding.removeButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                getResultBundle(
                    isKeeping = false,
                )
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "activate-account"
        const val ACTION_REQUEST = "token-action"
        private const val IS_KEEPING_KEY = "is_keeping"
        private const val TOKEN_NAME_KEY = "token_name"

        fun newInstance(bundle: Bundle) = NewlyReceivedTokenNoticeDialog().apply {
            arguments = bundle
        }

        fun getBundle(tokenName: String) = Bundle().apply {
            putString(TOKEN_NAME_KEY, tokenName)
        }

        private fun getResultBundle(isKeeping: Boolean) = Bundle().apply {
            putBoolean(IS_KEEPING_KEY, isKeeping)
        }

        /**
         * @return **true** if keeping the token.
         */
        fun getResult(bundle: Bundle): Boolean =
            bundle.getBoolean(IS_KEEPING_KEY, true)
    }
}
