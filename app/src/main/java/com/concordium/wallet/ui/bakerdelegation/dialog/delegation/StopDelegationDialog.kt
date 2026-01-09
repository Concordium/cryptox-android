package com.concordium.wallet.ui.bakerdelegation.dialog.delegation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.databinding.DialogStopDelegationBinding
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class StopDelegationDialog : BaseDialogFragment() {

    private lateinit var binding: DialogStopDelegationBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogStopDelegationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listOf(binding.closeButton, binding.goBackButton).forEach {
            it.setOnClickListener {
                setFragmentResult(
                    ACTION_KEY,
                    setResultBundle(false)
                )
                dismiss()
            }
        }

        binding.continueButton.setOnClickListener {
            setFragmentResult(
                ACTION_KEY,
                setResultBundle(true)
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "StopDelegationDialog"
        const val ACTION_KEY = "continue_to_remove"
        private const val CONTINUE = "continue"

        fun getResult(bundle: Bundle): Boolean = bundle.getBoolean(CONTINUE, false)

        private fun setResultBundle(isContinue: Boolean) = Bundle().apply {
            putBoolean(CONTINUE, isContinue)
        }
    }
}