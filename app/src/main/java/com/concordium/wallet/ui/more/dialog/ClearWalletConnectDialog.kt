package com.concordium.wallet.ui.more.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogClearWalletConnectBinding

class ClearWalletConnectDialog : AppCompatDialogFragment() {

    override fun getTheme(): Int = R.style.CCX_Dialog

    private lateinit var binding: DialogClearWalletConnectBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogClearWalletConnectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listOf(binding.cancelButton, binding.closeButton).forEach {
            it.setOnClickListener {
                setFragmentResult(
                    ACTION_REQUEST,
                    setResultBundle(false)
                )
                dismiss()
            }
        }

        binding.confirmButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                setResultBundle(true)
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "ClearWalletConnectDialog"
        const val ACTION_REQUEST = "clear_action"
        private const val IS_CLEARING = "is_clearing"

        private fun setResultBundle(isClearing: Boolean) = Bundle().apply {
            putBoolean(IS_CLEARING, isClearing)
        }

        fun getResult(bundle: Bundle): Boolean = bundle.getBoolean(IS_CLEARING, false)
    }
}