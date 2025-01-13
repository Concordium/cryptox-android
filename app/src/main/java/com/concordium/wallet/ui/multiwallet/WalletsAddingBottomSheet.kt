package com.concordium.wallet.ui.multiwallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentImportSeedPhraseBottomSheetBinding
import com.concordium.wallet.databinding.FragmentWalletsAddingBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class WalletsAddingBottomSheet : BottomSheetDialogFragment() {
    override fun getTheme(): Int = R.style.CCX_BottomSheetDialog

    private lateinit var binding: FragmentWalletsAddingBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding  = FragmentWalletsAddingBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        binding.okButton.setOnClickListener { dismiss() }
    }

    companion object {
        const val TAG = "wallets-adding"
    }
}
