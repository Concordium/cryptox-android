package com.concordium.wallet.ui.seed.setup

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityOneStepSetupWalletBinding
import com.concordium.wallet.databinding.ItemCcxSeedPhraseWordBinding
import com.concordium.wallet.extension.collect
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl

class OneStepSetupWalletActivity :
    BaseActivity(R.layout.activity_one_step_setup_wallet),
    AuthDelegate by AuthDelegateImpl() {

    private val binding: ActivityOneStepSetupWalletBinding by lazy {
        ActivityOneStepSetupWalletBinding.bind(findViewById(R.id.toastLayoutTopError))
    }
    private lateinit var viewModel: OneStepSetupWalletViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            SavedStateViewModelFactory(application, this)
        )[OneStepSetupWalletViewModel::class.java]

        initWords()
        initButtons()

        subscribeToEvents()
    }

    @SuppressLint("SetTextI18n")
    private fun initWords(
    ) = viewModel.phraseFlow.collectWhenStarted(this) { words ->
        binding.wordsLayout.removeAllViews()

        if (words == null) {
            return@collectWhenStarted
        }

        words.forEachIndexed { index, word ->
            val wordView = ItemCcxSeedPhraseWordBinding
                .inflate(layoutInflater, binding.wordsLayout, true)
            wordView.numberTextView.text = (index + 1).toString()
            wordView.wordTextView.text = word
        }
    }

    private fun initButtons() {
        binding.consentCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                App.appCore.tracker.seedPhraseCheckboxBoxChecked()
            }
            binding.continueButton.isEnabled = isChecked
        }

        binding.continueButton.setOnClickListener {
            App.appCore.tracker.seedPhraseContinueCLicked()
            viewModel.onContinueClicked()
        }

        binding.copyButton.setOnClickListener {
            App.appCore.tracker.seedPhraseCopyClicked()

            val clipboardManager: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText(
                getString(R.string.your_seed_phrase),
                viewModel.phraseString,
            )
            clipboardManager.setPrimaryClip(clipData)

            binding.copyButton.text = getString(R.string.setup_wallet_seed_phrase_copied)
            binding.copyButton.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null,
                null,
                ContextCompat.getDrawable(this, R.drawable.cryptox_ico_check_16),
                null,
            )
        }
    }

    private fun subscribeToEvents(
    ) = viewModel.eventsFlow.collect(this) { event ->
        when (event) {
            OneStepSetupWalletViewModel.Event.Authenticate -> {
                showAuthentication(
                    activity = this,
                    onAuthenticated = viewModel::onAuthenticated
                )
            }

            OneStepSetupWalletViewModel.Event.ShowFatalError -> {
                showError(R.string.setup_wallet_failed)
            }

            OneStepSetupWalletViewModel.Event.GoToAccountOverview -> {
                gotoAccountOverview()
            }
        }
    }

    private fun gotoAccountOverview() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }
}
