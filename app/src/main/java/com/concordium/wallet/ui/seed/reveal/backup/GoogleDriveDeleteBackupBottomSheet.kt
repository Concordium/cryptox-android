package com.concordium.wallet.ui.seed.reveal.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentGoogleDriveDeleteBackupBottomSheetBinding
import com.concordium.wallet.extension.showSingle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class GoogleDriveDeleteBackupBottomSheet : BottomSheetDialogFragment() {
    override fun getTheme(): Int = R.style.CCX_BottomSheetDialog

    private lateinit var binding: FragmentGoogleDriveDeleteBackupBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGoogleDriveDeleteBackupBottomSheetBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.deleteBackupButton.setOnClickListener {
            DeleteBackupConfirmDialog().showSingle(
                parentFragmentManager,
                DeleteBackupConfirmDialog.TAG
            )
        }
    }

    companion object {
        const val TAG = "GoogleDriveDeleteBackupBottomSheet"
    }
}