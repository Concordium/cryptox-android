package com.concordium.wallet.ui.passphrase.recover

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityRecoverWalletBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.passphrase.recoverprocess.RecoverProcessActivity
import com.concordium.wallet.util.KeyboardUtil

class RecoverWalletActivity :
    BaseActivity(R.layout.activity_recover_wallet, R.string.pass_phrase_recover_title),
    AuthDelegate by AuthDelegateImpl() {

    private val binding by lazy {
        ActivityRecoverWalletBinding.bind(findViewById(R.id.toastLayoutTopError))
    }
    val viewModel: PassPhraseRecoverViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(isVisible = true)
        setActionBarTitle("")
        initObservers()

        if (BuildConfig.DEBUG) {
            setActionBarTitle("[dev] click to skip")
            binding.toolbarLayout.toolbarTitle.isClickable = true
            binding.toolbarLayout.toolbarTitle.setOnClickListener {
                showAuthentication(this) { password ->
                    viewModel.setPredefinedPhraseForTesting(password)
                }
            }
        }
    }

    private fun initObservers() {
        viewModel.seed.observe(this) { seed ->
            showAuthentication(this) { password ->
                viewModel.setSeedPhrase(seed, password)
            }
        }

        viewModel.saveSeed.observe(this) { saveSuccess ->
            if (saveSuccess) {
                finish()
                startActivity(Intent(this, RecoverProcessActivity::class.java))
            } else {
                KeyboardUtil.hideKeyboard(this)
                showError(R.string.auth_login_seed_error)
            }
        }
    }
}
