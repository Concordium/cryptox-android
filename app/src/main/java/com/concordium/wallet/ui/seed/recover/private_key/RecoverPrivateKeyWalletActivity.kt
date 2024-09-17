package com.concordium.wallet.ui.seed.recover.private_key

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityRecoverPrivateKeyWalletBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.seed.recoverprocess.RecoverProcessActivity
import com.concordium.wallet.util.KeyboardUtil

class RecoverPrivateKeyWalletActivity :
    BaseActivity(R.layout.activity_recover_private_key_wallet, R.string.private_key_recover_title),
    AuthDelegate by AuthDelegateImpl() {

    private val binding by lazy {
        ActivityRecoverPrivateKeyWalletBinding.bind(findViewById(R.id.toastLayoutTopError))
    }

    val viewModel: PrivateKeyRecoverViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(isVisible = true)
        setActionBarTitle("")

        if (BuildConfig.DEBUG) {
            setActionBarTitle("[dev] click to skip")
            binding.toolbarLayout.toolbarTitle.isClickable = true
            binding.toolbarLayout.toolbarTitle.setOnClickListener {
                showAuthentication(this) { password ->
                    viewModel.setPredefinedKeyForTesting(password)
                }
            }
        }
        initViews()
        initObservers()
    }

    private fun initViews() {
        binding.continueButton.setOnClickListener {
            showAuthentication(this) { password ->
                viewModel.setPrivateKey(viewModel.privateKey.value, password)
            }
        }
    }

    private fun initObservers() {
        viewModel.validate.collectWhenStarted(this) { validateSuccess ->
            binding.continueButton.isVisible = validateSuccess
        }

        viewModel.saveKeySuccess.collectWhenStarted(this) { saveSuccess ->
            if (saveSuccess) {
                finish()
                startActivity(Intent(this, RecoverProcessActivity::class.java))
            } else {
                KeyboardUtil.hideKeyboard(this)
            }
        }
    }
}