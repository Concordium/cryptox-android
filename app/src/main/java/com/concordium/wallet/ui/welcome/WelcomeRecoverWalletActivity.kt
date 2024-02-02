package com.concordium.wallet.ui.welcome

import android.content.Intent
import android.os.Bundle
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityWelcomeRecoverWalletBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.more.import.ImportActivity
import com.concordium.wallet.ui.passphrase.recover.RecoverWalletActivity

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
        binding.usePhraseLayout.setOnClickListener {
            goToPhraseRecovery()
        }

        binding.useFileLayout.setOnClickListener {
            goToImport()
        }
    }

    private fun goToImport() {
        startActivity(
            Intent(this, ImportActivity::class.java).apply {
                putExtra(ImportActivity.EXTRA_GO_TO_ACCOUNTS_OVERVIEW_ON_SUCCESS, true)
            }
        )
    }

    private fun goToPhraseRecovery() {
        startActivity(Intent(this, RecoverWalletActivity::class.java))
    }
}
