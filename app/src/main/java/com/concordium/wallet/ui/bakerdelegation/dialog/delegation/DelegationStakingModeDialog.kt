package com.concordium.wallet.ui.bakerdelegation.dialog.delegation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.databinding.DialogDelegationStakingModeBinding
import com.concordium.wallet.uicore.dialog.BaseGradientDialogFragment

class DelegationStakingModeDialog : BaseGradientDialogFragment() {

    private lateinit var binding: DialogDelegationStakingModeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogDelegationStakingModeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.laterButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                setResultBundle(isSelect = false)
            )
            dismiss()
        }

        binding.selectButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                setResultBundle(isSelect = true)
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "DelegationStakingModeDialog"
        const val ACTION_REQUEST = "select_action"
        private const val SELECT = "select"

        fun getResult(bundle: Bundle): Boolean = bundle.getBoolean(SELECT, false)

        private fun setResultBundle(isSelect: Boolean) = Bundle().apply {
            putBoolean(SELECT, isSelect)
        }
    }
}