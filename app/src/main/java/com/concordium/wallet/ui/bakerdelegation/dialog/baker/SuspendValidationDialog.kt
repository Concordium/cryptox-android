package com.concordium.wallet.ui.bakerdelegation.dialog.baker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.databinding.DialogSuspendValidationBinding
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class SuspendValidationDialog : BaseDialogFragment() {

    private lateinit var binding: DialogSuspendValidationBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogSuspendValidationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listOf(binding.closeButton, binding.goBackButton).forEach {
            it.setOnClickListener {
                setFragmentResult(
                    ACTION_SUSPEND_CONTINUE,
                    setResultBundle(isContinue = false)
                )
                dismiss()
            }
        }

        binding.continueButton.setOnClickListener {
            setFragmentResult(
                ACTION_SUSPEND_CONTINUE,
                setResultBundle(isContinue = true)
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "SuspendValidationDialog"
        const val ACTION_SUSPEND_CONTINUE = "action_suspend_continue"
        private const val CONTINUE = "continue"

        fun getResult(bundle: Bundle): Boolean = bundle.getBoolean(CONTINUE, false)

        private fun setResultBundle(isContinue: Boolean) = Bundle().apply {

            putBoolean(CONTINUE, isContinue)
        }
    }
}