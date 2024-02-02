package com.concordium.wallet.ui.passphrase.recoverprocess

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityRecoverProcessBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl

class RecoverProcessActivity : BaseActivity(
    R.layout.activity_recover_process,
    R.string.pass_phrase_recover_process_title,
), AuthDelegate by AuthDelegateImpl() {
    private val binding by lazy {
        ActivityRecoverProcessBinding.bind(findViewById(R.id.toastLayoutTopError))
    }
    private lateinit var viewModel: RecoverProcessViewModel
    private var passwordSet = false
    private var showForFirstRecovery = true

    companion object {
        const val SHOW_FOR_FIRST_RECOVERY = "SHOW_FOR_FIRST_RECOVERY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showForFirstRecovery = intent.extras?.getBoolean(SHOW_FOR_FIRST_RECOVERY, true) ?: true

        hideActionBarBack(isVisible = !showForFirstRecovery)
        setActionBarTitle("")

        initializeViewModel()
        initViews()
        initObservers()
    }

    override fun onBackPressed() {
        // Ignore back press
        if (!showForFirstRecovery)
            super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stop()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[RecoverProcessViewModel::class.java]
    }

    private fun initViews() {
        initButtons()
    }

    private fun initButtons() {
        binding.continueButton.setOnClickListener {
            if (passwordSet) {
                finishAffinity()
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            } else {
                startScanning()
            }
        }
        binding.tryAgainButton.setOnClickListener {
            startScanning()
        }
    }

    private fun initObservers() {
        viewModel.statusChanged.observe(this) {
            runOnUiThread {
                finishScanningView()
            }
        }
        viewModel.errorLiveData.observe(this) {
            runOnUiThread {
                showError(it)
            }
        }
    }

    private fun finishScanningView() {
        supportFragmentManager.beginTransaction().replace(
            R.id.fragment_container,
            RecoverProcessFinishedFragment.newInstance(viewModel.recoverProcessData),
            null
        ).commit()
        binding.continueButton.visibility = View.VISIBLE
        if (viewModel.recoverProcessData.noResponseFrom.size > 0) {
            binding.tryAgainButton.visibility = View.VISIBLE
            binding.continueButton.text = getString(R.string.pass_phrase_recover_process_continue)
        } else {
            binding.tryAgainButton.visibility = View.GONE
            binding.continueButton.text =
                getString(R.string.pass_phrase_recover_process_continue_to_wallet)
        }
    }

    private fun startScanning() {
        showAuthentication(this) { password ->
            passwordSet = true
            runOnUiThread {
                scanningView(password)
            }
        }
    }

    private fun scanningView(password: String) {
        supportFragmentManager.beginTransaction().replace(
            R.id.fragment_container,
            RecoverProcessScanningFragment.newInstance(
                viewModel,
                viewModel.recoverProcessData,
                password
            ),
            null
        ).commit()
        binding.continueButton.visibility = View.GONE
        binding.tryAgainButton.visibility = View.GONE
    }
}
