package com.concordium.wallet.ui.account.accountsoverview

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.ui.seed.reveal.SavedSeedPhraseRevealActivity
import com.concordium.wallet.uicore.dialog.BaseDialogFragment

class SeedPhraseBackupNoticeDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.seed_phrase_backup_notice_title),
            description = getString(R.string.seed_phrase_backup_notice_message),
            okButtonText = getString(R.string.seed_phrase_backup_notice_backup),
            cancelButtonText = getString(R.string.seed_phrase_backup_notice_hide_anyway)
        )

        binding.okButton.setOnClickListener {
            startActivity(Intent(requireActivity(), SavedSeedPhraseRevealActivity::class.java))
            dismiss()
        }
        binding.cancelButton.setOnClickListener {
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
