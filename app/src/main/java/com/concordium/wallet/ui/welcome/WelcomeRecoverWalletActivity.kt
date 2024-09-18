package com.concordium.wallet.ui.welcome

import android.content.Intent
import android.os.Bundle
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityWelcomeRecoverWalletBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.base.BaseActivity
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
    }

    private fun initViews() {
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
        ImportFileWalletDialog().showSingle(
            supportFragmentManager,
            ImportFileWalletDialog.TAG
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
}
