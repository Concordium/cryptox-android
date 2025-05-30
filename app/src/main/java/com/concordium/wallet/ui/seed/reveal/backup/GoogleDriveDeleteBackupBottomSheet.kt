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
    private val backupCreationTime: String by lazy {
        arguments?.getString(BACKUP_CREATION_TIME) ?: ""
    }

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
        binding.backupTime.text = requireContext().getString(
            R.string.settings_overview_google_drive_backup_time,
            backupCreationTime
        )

        binding.deleteBackupButton.setOnClickListener {
            DeleteBackupConfirmDialog().showSingle(
                parentFragmentManager,
                DeleteBackupConfirmDialog.TAG
            )
        }

        parentFragmentManager.setFragmentResultListener(
            DeleteBackupConfirmDialog.DELETE_ACTION_REQUEST,
            viewLifecycleOwner
        ) { _, bundle ->
            if (DeleteBackupConfirmDialog.getResult(bundle)) {
                parentFragmentManager.setFragmentResult(
                    ACTION_REQUEST,
                    setResultBundle(true)
                )
                dismiss()
            }
        }
    }

    companion object {
        const val TAG = "GoogleDriveDeleteBackupBottomSheet"
        const val ACTION_REQUEST = "google_drive_delete_backup_action_request"
        private const val BACKUP_CREATION_TIME = "backup_creation_time"
        private const val IS_DELETING = "is_deleting"

        fun newInstance(bundle: Bundle) = GoogleDriveDeleteBackupBottomSheet().apply {
            arguments = bundle
        }

        fun setBundle(backupCreationTime: String) = Bundle().apply {
            putString(BACKUP_CREATION_TIME, backupCreationTime)
        }

        private fun setResultBundle(isDeleting: Boolean) = Bundle().apply {
            putBoolean(IS_DELETING, isDeleting)
        }

        fun getResult(bundle: Bundle): Boolean = bundle.getBoolean(IS_DELETING, false)

    }
}