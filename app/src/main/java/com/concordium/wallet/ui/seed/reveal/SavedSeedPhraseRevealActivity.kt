package com.concordium.wallet.ui.seed.reveal

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.ViewOutlineProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivitySavedSeedPhraseRevealBinding
import com.concordium.wallet.databinding.ItemCcxSeedPhraseWordBinding
import com.concordium.wallet.extension.collect
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl

class SavedSeedPhraseRevealActivity :
    BaseActivity(R.layout.activity_saved_seed_phrase_reveal),
    AuthDelegate by AuthDelegateImpl() {

    private val binding: ActivitySavedSeedPhraseRevealBinding by lazy {
        ActivitySavedSeedPhraseRevealBinding.bind(findViewById(R.id.toastLayoutTopError))
    }
    private lateinit var viewModel: SavedSeedPhraseRevealViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get()

        setActionBarTitle("")
        hideActionBarBack(isVisible = true)

        initWords()
        initBlur()
        initButtons()

        subscribeToEvents()
        subscribeToState()
    }

    @SuppressLint("SetTextI18n")
    private fun initWords(
    ) = viewModel.phraseFlow.collectWhenStarted(this) { words ->
        binding.contentLayout.removeAllViews()

        words.forEachIndexed { index, word ->
            val wordView = ItemCcxSeedPhraseWordBinding
                .inflate(layoutInflater, binding.contentLayout, true)
            wordView.numberTextView.text = (index + 1).toString()
            wordView.wordTextView.text = word
        }
    }

    private fun initBlur() = with(binding.blurView) {
        setupWith(binding.wordsRootLayout)
        setBlurRadius(2f)

        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = true
    }

    private fun initButtons() {
        binding.showButton.setOnClickListener {
            viewModel.onShowPhraseClicked()
        }

        binding.copyButton.setOnClickListener {
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

        binding.consentCheckBox.isVisible = viewModel.isBackupConfirmationVisible
        binding.consentCheckBox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onConsentCheckboxClicked(isChecked)
        }

        binding.continueButton.isVisible = viewModel.isBackupConfirmationVisible
        binding.continueButton.setOnClickListener {
            viewModel.onContinueClicked()
        }
    }

    private fun subscribeToEvents(
    ) = viewModel.eventsFlow.collect(this) { event ->

        when (event) {
            SavedSeedPhraseRevealViewModel.Event.Authenticate -> {
                showAuthentication(
                    activity = this,
                    onAuthenticated = viewModel::onAuthenticated
                )
            }

            SavedSeedPhraseRevealViewModel.Event.ShowFatalError -> {
                showError(R.string.saved_seed_phrase_reveal_failed)
            }

            SavedSeedPhraseRevealViewModel.Event.Finish -> {
                finish()
            }
        }
    }

    private fun subscribeToState() {
        viewModel.stateFlow.collectWhenStarted(this) { state ->

            with(binding.blurView) {
                isVisible = state is SavedSeedPhraseRevealViewModel.State.Hidden
                setBlurEnabled(isVisible)
            }

            binding.copyButton.isVisible = state is SavedSeedPhraseRevealViewModel.State.Revealed
            binding.showButton.isVisible = state is SavedSeedPhraseRevealViewModel.State.Hidden
        }

        viewModel.isConsentCheckBoxEnabledFlow.collectWhenStarted(
            this,
            binding.consentCheckBox::setEnabled,
        )

        viewModel.isContinueButtonEnabledFlow.collectWhenStarted(
            this,
            binding.continueButton::setEnabled,
        )
    }

    override fun loggedOut() {
        // No need to show auth, as it is anyway requested further.
    }
}
