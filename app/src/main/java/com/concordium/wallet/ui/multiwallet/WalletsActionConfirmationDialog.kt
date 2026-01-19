package com.concordium.wallet.ui.multiwallet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class WalletsActionConfirmationDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFragmentResult(
            CONFIRMATION_REQUEST,
            getResultBundle(
                isConfirmed = false,
            )
        )

        setViews(
            title = getString(requireNotNull(arguments?.getInt(TITLE_ID_EXTRA))),
            description = getString(requireNotNull(arguments?.getInt(DETAILS_ID_EXTRA))),
            okButtonText = getString(requireNotNull(arguments?.getInt(OK_BUTTON_TEXT_ID_EXTRA))),
            cancelButtonText = getString(R.string.wallets_action_go_back)
        )

        listOf(binding.closeButton, binding.cancelButton).forEach {
            it.setOnClickListener {
                dismiss()
            }
        }

        binding.okButton.setOnClickListener {
            setFragmentResult(
                CONFIRMATION_REQUEST,
                getResultBundle(
                    isConfirmed = true,
                )
            )
            dismiss()
        }
    }

    private fun getResultBundle(
        isConfirmed: Boolean
    ) = Bundle().apply {
        putBoolean(IS_CONFIRMED_EXTRA, isConfirmed)
        putString(ACTION_EXTRA, arguments?.getString(ACTION_EXTRA))
    }

    data class Result(
        val action: String,
        val isConfirmed: Boolean,
    )

    companion object {
        const val TAG = "wallets-action-confirmation"
        const val CONFIRMATION_REQUEST = "confirmation"
        const val ADDING_SEED_WALLET_ACTION = "adding_seed_wallet"
        const val ADDING_FILE_WALLET_ACTION = "adding_file_wallet"
        const val IMPORTING_FILE_WALLET_ACTION = "importing_file_wallet"
        const val REMOVING_WALLET_ACTION = "removing_wallet"
        private const val IS_CONFIRMED_EXTRA = "is_confirmed"
        private const val ACTION_EXTRA = "action"
        private const val IMAGE_ID_EXTRA = "image_id"
        private const val TITLE_ID_EXTRA = "title_id"
        private const val DETAILS_ID_EXTRA = "details_id"
        private const val OK_BUTTON_TEXT_ID_EXTRA = "ok_button_text"

        fun addingSeedWallet() = WalletsActionConfirmationDialog().apply {
            arguments = Bundle().apply {
                putInt(IMAGE_ID_EXTRA, R.drawable.ccx_file_wallet) // Yes.
                putInt(TITLE_ID_EXTRA, R.string.wallets_action_adding_seed_wallet)
                putInt(DETAILS_ID_EXTRA, R.string.wallets_action_adding_seed_wallet_explanation)
                putInt(OK_BUTTON_TEXT_ID_EXTRA, R.string.wallets_action_add_wallet)
                putString(ACTION_EXTRA, ADDING_SEED_WALLET_ACTION)
            }
        }

        fun addingFileWallet() = WalletsActionConfirmationDialog().apply {
            arguments = Bundle().apply {
                putInt(IMAGE_ID_EXTRA, R.drawable.ccx_warning)
                putInt(TITLE_ID_EXTRA, R.string.wallets_action_adding_file_wallet)
                putInt(DETAILS_ID_EXTRA, R.string.wallets_action_adding_file_wallet_explanation)
                putInt(OK_BUTTON_TEXT_ID_EXTRA, R.string.wallets_action_add_wallet)
                putString(ACTION_EXTRA, ADDING_FILE_WALLET_ACTION)
            }
        }

        fun importingFileWallet() = WalletsActionConfirmationDialog().apply {
            arguments = Bundle().apply {
                putInt(IMAGE_ID_EXTRA, R.drawable.ccx_warning)
                putInt(TITLE_ID_EXTRA, R.string.wallets_action_importing_file_wallet)
                putInt(DETAILS_ID_EXTRA, R.string.wallets_action_importing_file_wallet_explanation)
                putInt(OK_BUTTON_TEXT_ID_EXTRA, R.string.wallets_action_import_wallet)
                putString(ACTION_EXTRA, IMPORTING_FILE_WALLET_ACTION)
            }
        }

        fun removingWallet() = WalletsActionConfirmationDialog().apply {
            arguments = Bundle().apply {
                putInt(IMAGE_ID_EXTRA, R.drawable.ccx_disconnect)
                putInt(TITLE_ID_EXTRA, R.string.wallets_action_removing_wallet)
                putInt(DETAILS_ID_EXTRA, R.string.wallets_action_removing_wallet_explanation)
                putInt(OK_BUTTON_TEXT_ID_EXTRA, R.string.wallets_action_remove_wallet)
                putString(ACTION_EXTRA, REMOVING_WALLET_ACTION)
            }
        }

        fun getResult(bundle: Bundle) = Result(
            isConfirmed = bundle.getBoolean(IS_CONFIRMED_EXTRA, false),
            action = bundle.getString(ACTION_EXTRA)!!,
        )
    }
}
