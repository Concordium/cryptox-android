package com.concordium.wallet.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentWelcomePromoActivateAccountBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class WelcomeActivateAccountBottomSheet : BottomSheetDialogFragment() {
    override fun getTheme(): Int =
        R.style.CCX_BottomSheetDialog

    private lateinit var binding: FragmentWelcomePromoActivateAccountBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWelcomePromoActivateAccountBottomSheetBinding
            .inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.createWalletButton.setOnClickListener {
            App.appCore.tracker.welcomeActivateAccountDialogCreateClicked()
            setFragmentResult(
                ACTION_REQUEST,
                getResultBundle(ChosenAction.CREATE)
            )
            dismiss()
        }

        binding.importWalletButton.setOnClickListener {
            App.appCore.tracker.welcomeActivateAccountDialogImportClicked()
            setFragmentResult(
                ACTION_REQUEST,
                getResultBundle(ChosenAction.IMPORT)
            )
            dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        App.appCore.tracker.welcomeActivateAccountDialog()
    }

    enum class ChosenAction {
        CREATE,
        IMPORT,
        ;
    }

    companion object {
        const val TAG = "activate-account"
        const val ACTION_REQUEST = "activate-account-action"
        private const val CHOSEN_ACTION_KEY = "chosen_action"

        private fun getResultBundle(chosenAction: ChosenAction) = Bundle().apply {
            putInt(CHOSEN_ACTION_KEY, chosenAction.ordinal)
        }

        fun getResult(bundle: Bundle): ChosenAction =
            ChosenAction.values()[bundle.getInt(CHOSEN_ACTION_KEY)]
    }
}
