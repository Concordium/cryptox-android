package com.concordium.wallet.ui.multiwallet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentSeedWalletAddingBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SeedWalletAddingBottomSheet : BottomSheetDialogFragment() {
    override fun getTheme(): Int = R.style.CCX_BottomSheetDialog

    private lateinit var binding: FragmentSeedWalletAddingBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSeedWalletAddingBottomSheetBinding
            .inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener { dialogInterface ->
                (dialogInterface as? BottomSheetDialog)
                    ?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?.let { BottomSheetBehavior.from(it) }
                    ?.also { bottomSheetBehavior ->
                        // Automatically expand the sheet to show as much content as possible.
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        // Prevent returning to the collapsed one for better dismiss experience
                        bottomSheetBehavior.skipCollapsed = true
                    }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.createWalletButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                getResultBundle(ChosenAction.CREATE)
            )
            dismiss()
        }
        binding.importWalletButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                getResultBundle(ChosenAction.IMPORT)
            )
            dismiss()
        }
    }

    enum class ChosenAction {
        CREATE,
        IMPORT,
        ;
    }

    companion object {
        const val TAG = "seed-wallet-adding"
        const val ACTION_REQUEST = "seed-wallet-adding-action"
        private const val CHOSEN_ACTION_KEY = "chosen_action"

        private fun getResultBundle(chosenAction: ChosenAction) = Bundle().apply {
            putInt(CHOSEN_ACTION_KEY, chosenAction.ordinal)
        }

        fun getResult(bundle: Bundle): ChosenAction =
            ChosenAction.values()[bundle.getInt(CHOSEN_ACTION_KEY)]
    }
}
