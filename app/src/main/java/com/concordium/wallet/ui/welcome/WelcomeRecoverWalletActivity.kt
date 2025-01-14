package com.concordium.wallet.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityWelcomeRecoverWalletBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.more.import.ImportActivity
import com.concordium.wallet.ui.multiwallet.WalletsActionConfirmationDialog
import com.concordium.wallet.ui.seed.recover.RecoverSeedPhraseWalletActivity
import com.concordium.wallet.ui.seed.recover.seed.RecoverSeedWalletActivity

class WelcomeRecoverWalletActivity : BaseActivity(
    R.layout.activity_welcome_recover_wallet,
    R.string.welcome_recover_title
) {
    private val binding by lazy {
        ActivityWelcomeRecoverWalletBinding.bind(findViewById(R.id.root_layout))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViews()

        hideActionBarBack(isVisible = true)
        setActionBarTitle("")

        supportFragmentManager.setFragmentResultListener(
            WalletsActionConfirmationDialog.CONFIRMATION_REQUEST,
            this,
        ) { _, bundle ->
            if (WalletsActionConfirmationDialog.getResult(bundle).isConfirmed) {
                startActivity(
                    Intent(this, ImportActivity::class.java).apply {
                        putExtra(ImportActivity.EXTRA_GO_TO_ACCOUNTS_OVERVIEW_ON_SUCCESS, true)
                    }
                )
            }
        }
    }

    private fun initViews() {
        binding.usePhraseLayout.isVisible =
            intent.getBooleanExtra(EXTRA_SHOW_SEED_OPTIONS, true)
        binding.useFileLayout.isVisible =
            intent.getBooleanExtra(EXTRA_SHOW_FILE_OPTIONS, true)
        binding.bottomAwareLayout.isVisible = binding.useFileLayout.isVisible

        binding.importSeedPhraseButton.setOnClickListener {
            goToPhraseRecovery()
        }

        binding.importSeedButton.setOnClickListener {
            goToSeedRecovery()
        }

        binding.importBackupFileButton.setOnClickListener {
            showImportFileWalletDialog()
        }

        binding.fileWalletInfoIcon.setOnClickListener {
            showImportFileInfo()
        }

        binding.seedPhraseInfoIcon.setOnClickListener {
            showImportSeedPhraseInfo()
        }
    }

    private fun showImportFileWalletDialog() {
        WalletsActionConfirmationDialog.importingFileWallet().showSingle(
            supportFragmentManager,
            WalletsActionConfirmationDialog.TAG,
        )
    }

    private fun showImportFileInfo() {
        ImportBackupFileInfoBottomSheet().showSingle(
            supportFragmentManager,
            ImportBackupFileInfoBottomSheet.TAG
        )
    }

    private fun showImportSeedPhraseInfo() {
        ImportSeedPhraseInfoBottomSheet().showSingle(
            supportFragmentManager,
            ImportSeedPhraseInfoBottomSheet.TAG
        )
    }

    private fun goToPhraseRecovery() {
        startActivity(Intent(this, RecoverSeedPhraseWalletActivity::class.java))
    }

    private fun goToSeedRecovery() {
        startActivity(Intent(this, RecoverSeedWalletActivity::class.java))
    }

    companion object {
        private const val EXTRA_SHOW_SEED_OPTIONS = "show_seed_options"
        private const val EXTRA_SHOW_FILE_OPTIONS = "show_file_options"

        fun getBundle(
            showSeedOptions: Boolean = true,
            showFileOptions: Boolean = true,
        ) = Bundle().apply {
            require(showFileOptions || showSeedOptions) {
                "No options to show"
            }

            putBoolean(EXTRA_SHOW_SEED_OPTIONS, showSeedOptions)
            putBoolean(EXTRA_SHOW_FILE_OPTIONS, showFileOptions)
        }
    }
}
