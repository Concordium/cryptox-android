package com.concordium.wallet.ui.bakerdelegation.dialog.delegation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.databinding.DialogValidationErrorBinding
import com.concordium.wallet.uicore.dialog.BaseGradientDialogFragment

class DelegationErrorDialog : BaseGradientDialogFragment() {

    private lateinit var binding: DialogValidationErrorBinding
    private val errorMessage: String by lazy { arguments?.getString(ERROR_MESSAGE)?: "" }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogValidationErrorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.detailsTextView.text = errorMessage

        binding.tryAgainButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                getResultBundle(tryAgain = true)
            )
            dismiss()
        }

        binding.laterButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                getResultBundle(tryAgain = false)
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "DelegationErrorDialog"
        const val ACTION_REQUEST = "delegation_try_again_action"
        private const val ERROR_MESSAGE = "delegation_error_message"
        private const val TRY_AGAIN = "try_again"

        fun newInstance(bundle: Bundle) = DelegationErrorDialog().apply {
            arguments = bundle
        }

        fun setBundle(errorMessage: String) = Bundle().apply {
            putString(ERROR_MESSAGE, errorMessage)
        }

        fun getResult(bundle: Bundle): Boolean = bundle.getBoolean(TRY_AGAIN, false)

        private fun getResultBundle(tryAgain: Boolean) = Bundle().apply {
            putBoolean(TRY_AGAIN, tryAgain)
        }
    }
}