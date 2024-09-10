package com.concordium.wallet.ui.passphrase.reveal

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
    }

    private fun initSeed() {
        viewModel.seedFlow.collectWhenStarted(this) { seedHex ->
            binding.seedTextView.text = seedHex
        }

        viewModel.stateFlow.collectWhenStarted(this) { state ->
            // Do not allow selection through blur,
            // even though the content is not the actual seed until it is revealed.
            binding.seedTextView.setTextIsSelectable(state is SavedSeedRevealViewModel.State.Revealed)
        }
    }

    private fun initBlur() = with(binding.blurView) {
        setupWith(binding.wordsRootLayout)
        setBlurRadius(2f)

        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = true

        viewModel.stateFlow.collectWhenStarted(this@SavedSeedRevealActivity) { state ->
            isVisible = state is SavedSeedRevealViewModel.State.Hidden
            setBlurEnabled(isVisible)
        }
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

        viewModel.stateFlow.collectWhenStarted(this) { state ->
            binding.copyButton.isVisible = state is SavedSeedRevealViewModel.State.Revealed
            binding.showButton.isVisible = state is SavedSeedRevealViewModel.State.Hidden
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

    override fun loggedOut() {
        // No need to show auth, as it is anyway requested further.
    }
}
