package com.concordium.wallet.ui.passphrase.reveal

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.ViewOutlineProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivitySavedPassPhraseRevealBinding
import com.concordium.wallet.databinding.ItemCcxSeedPhraseWordBinding
import com.concordium.wallet.extension.collect
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl

class SavedPassPhraseRevealActivity :
    BaseActivity(R.layout.activity_saved_pass_phrase_reveal),
    AuthDelegate by AuthDelegateImpl() {

    private val binding: ActivitySavedPassPhraseRevealBinding by lazy {
        ActivitySavedPassPhraseRevealBinding.bind(findViewById(R.id.toastLayoutTopError))
    }
    private lateinit var viewModel: SavedPassPhraseRevealViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[SavedPassPhraseRevealViewModel::class.java]

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
        binding.wordsLayout.removeAllViews()

        words.forEachIndexed { index, word ->
            val wordView = ItemCcxSeedPhraseWordBinding
                .inflate(layoutInflater, binding.wordsLayout, true)
            wordView.numberTextView.text = (index + 1).toString()
            wordView.wordTextView.text = word
        }
    }

    private fun initBlur() = with(binding.wordsBlurView) {
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
                getString(R.string.pass_phrase_title),
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
            SavedPassPhraseRevealViewModel.Event.Authenticate -> {
                showAuthentication(
                    activity = this,
                    onAuthenticated = viewModel::onAuthenticated
                )
            }

            SavedPassPhraseRevealViewModel.Event.Finish -> {
                setResult(Activity.RESULT_OK)
                finish()
            }

            SavedPassPhraseRevealViewModel.Event.ShowFatalError -> {
                showError(R.string.saved_pass_phrase_reveal_failed)
            }
        }
    }

    private fun subscribeToState(
    ) = viewModel.stateFlow.collectWhenStarted(this) { state ->

        with (binding.wordsBlurView) {
            isVisible = state is SavedPassPhraseRevealViewModel.State.Hidden
            setBlurEnabled(isVisible)
        }

        binding.copyButton.isVisible = state is SavedPassPhraseRevealViewModel.State.Revealed
        binding.showButton.isVisible = state is SavedPassPhraseRevealViewModel.State.Hidden
    }

    override fun loggedOut() {
        // No need to show auth, as it is anyway requested further.
    }
}
