package com.concordium.wallet.ui.more.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogRemoveWalletBinding

class RemoveWalletDialog : AppCompatDialogFragment() {

    override fun getTheme(): Int = R.style.CCX_Dialog

    private lateinit var binding: DialogRemoveWalletBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogRemoveWalletBinding.inflate(inflater, container, false)
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
        const val TAG = "RemoveWalletDialog"
        const val ACTION_REQUEST = "remove_action"
        private const val IS_REMOVING = "is_removing"

        private fun setResultBundle(isRemoving: Boolean) = Bundle().apply {
            putBoolean(IS_REMOVING, isRemoving)
        }

        fun getResult(bundle: Bundle): Boolean = bundle.getBoolean(IS_REMOVING, false)
    }
}