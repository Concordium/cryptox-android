package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogHidingTokenBinding

class HidingTokenDialog : AppCompatDialogFragment() {

    override fun getTheme(): Int = R.style.CCX_Dialog

    private lateinit var binding: DialogHidingTokenBinding

    private val tokenName: String by lazy {
        requireNotNull(arguments?.getString(TOKEN_NAME)) {
            "No $TOKEN_NAME specified"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogHidingTokenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.detailsTextView.text = getString(
            R.string.cis_hide_token_body,
            tokenName
        )

        listOf(binding.denyButton, binding.closeButton).forEach {
            it.setOnClickListener {
                setFragmentResult(
                    ACTION_REQUEST,
                    getResultBundle(isHiding = false)
                )
                dismiss()
            }
        }

        binding.hideButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                getResultBundle(isHiding = true)
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "hiding_token"
        const val ACTION_REQUEST = "hiding_action"
        private const val TOKEN_NAME = "token_name"
        private const val IS_HIDING = "is_hiding"

        fun newInstance(bundle: Bundle) = HidingTokenDialog().apply {
            arguments = bundle
        }

        fun getBundle(tokenName: String) = Bundle().apply {
            putString(TOKEN_NAME, tokenName)
        }

        private fun getResultBundle(isHiding: Boolean) = Bundle().apply {
            putBoolean(IS_HIDING, isHiding)
        }

        fun getResult(bundle: Bundle): Boolean = bundle.getBoolean(IS_HIDING, false)
    }
}