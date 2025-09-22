package com.concordium.wallet.ui.account.accountsoverview

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogSeedPhraseBackupNoticeBinding
import com.concordium.wallet.ui.seed.reveal.SavedSeedPhraseRevealActivity

class SeedPhraseBackupNoticeDialog : AppCompatDialogFragment() {

    override fun getTheme(): Int =
        R.style.CCX_Dialog

    private lateinit var binding: DialogSeedPhraseBackupNoticeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DialogSeedPhraseBackupNoticeBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        binding.backupButton.setOnClickListener {
            startActivity(Intent(requireActivity(), SavedSeedPhraseRevealActivity::class.java))
            dismiss()
        }
        binding.hideButton.setOnClickListener {
            App
                .appCore
                .session
                .walletStorage
                .setupPreferences
                .setRequireSeedPhraseBackupConfirmation(false)
            setFragmentResult(
                CONFIRMATION_REQUEST,
                Bundle().apply {
                    putBoolean(IS_HIDING_CONFIRMED_EXTRA, true)
                }
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "seed-phrase-backup-notice"
        const val CONFIRMATION_REQUEST = "confirm-hiding"
        private const val IS_HIDING_CONFIRMED_EXTRA = "is-hiding-confirmed"

        fun isHidingConfirmed(result: Bundle): Boolean =
            result.getBoolean(IS_HIDING_CONFIRMED_EXTRA, false)
    }
}
