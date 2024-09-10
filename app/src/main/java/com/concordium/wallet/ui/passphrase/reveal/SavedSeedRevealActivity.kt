package com.concordium.wallet.ui.passphrase.reveal

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
import com.concordium.wallet.databinding.ActivitySavedSeedRevealBinding
import com.concordium.wallet.extension.collect
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl

class SavedSeedRevealActivity :
    BaseActivity(R.layout.activity_saved_seed_reveal),
    AuthDelegate by AuthDelegateImpl() {

    private val binding: ActivitySavedSeedRevealBinding by lazy {
        ActivitySavedSeedRevealBinding.bind(findViewById(R.id.toastLayoutTopError))
    }
    private lateinit var viewModel: SavedSeedRevealViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get()

        setActionBarTitle("")
        hideActionBarBack(isVisible = true)

        initSeed()
        initBlur()
        initButtons()

        subscribeToEvents()
        subscribeToState()
    }

    @SuppressLint("SetTextI18n")
    private fun initSeed(
    ) = viewModel.seedFlow.collectWhenStarted(this) { seedHex ->
        binding.seedTextView.text = seedHex
    }

    private fun initBlur() = with(binding.blurView) {
        setupWith(binding.wordsRootLayout)
        setBlurRadius(2f)

        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = true
    }

    private fun initButtons() {
        binding.showButton.setOnClickListener {
            viewModel.onShowSeedClicked()
        }

        binding.copyButton.setOnClickListener {
            val clipboardManager: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText(
                getString(R.string.your_wallet_private_key),
                viewModel.seedString,
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
            SavedSeedRevealViewModel.Event.Authenticate -> {
                showAuthentication(
                    activity = this,
                    onAuthenticated = viewModel::onAuthenticated
                )
            }

            SavedSeedRevealViewModel.Event.ShowFatalError -> {
                showError(R.string.saved_seed_reveal_failed)
            }
        }
    }

    private fun subscribeToState(
    ) = viewModel.stateFlow.collectWhenStarted(this) { state ->

        with(binding.blurView) {
            isVisible = state is SavedSeedRevealViewModel.State.Hidden
            setBlurEnabled(isVisible)
        }

        binding.copyButton.isVisible = state is SavedSeedRevealViewModel.State.Revealed
        binding.showButton.isVisible = state is SavedSeedRevealViewModel.State.Hidden
    }

    override fun loggedOut() {
        // No need to show auth, as it is anyway requested further.
    }
}
