package com.concordium.wallet.ui.multiwallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogWalletsActionConfirmationBinding

class WalletsActionConfirmationDialog : AppCompatDialogFragment() {
    override fun getTheme(): Int =
        R.style.CCX_Dialog

    private lateinit var binding: DialogWalletsActionConfirmationBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogWalletsActionConfirmationBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setFragmentResult(
            CONFIRMATION_REQUEST,
            getResultBundle(
                isConfirmed = false,
            )
        )

        binding.imageView.setImageResource(requireNotNull(arguments?.getInt(IMAGE_ID_EXTRA)))
        binding.titleTextView.setText(requireNotNull(arguments?.getInt(TITLE_ID_EXTRA)))
        binding.detailsTextView.setText(requireNotNull(arguments?.getInt(DETAILS_ID_EXTRA)))
        binding.okButton.setText(requireNotNull(arguments?.getInt(OK_BUTTON_TEXT_ID_EXTRA)))

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
