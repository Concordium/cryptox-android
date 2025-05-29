package com.concordium.wallet.ui.seed.reveal

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewOutlineProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.core.backup.GoogleDriveManager
import com.concordium.wallet.databinding.ActivitySavedSeedPhraseRevealBinding
import com.concordium.wallet.databinding.ItemCcxSeedPhraseWordBinding
import com.concordium.wallet.extension.collect
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.common.delegates.GoogleSignInDelegate
import com.concordium.wallet.ui.common.delegates.GoogleSignInDelegateImpl
import com.concordium.wallet.ui.seed.reveal.backup.GoogleDriveCreateBackupActivity
import com.concordium.wallet.ui.seed.reveal.backup.GoogleDriveCreateBackupViewModel
import com.concordium.wallet.ui.seed.reveal.backup.GoogleDriveDeleteBackupBottomSheet
import com.concordium.wallet.util.DateTimeUtil.formatTo
import com.concordium.wallet.util.DateTimeUtil.toDate

class SavedSeedPhraseRevealActivity :
    BaseActivity(R.layout.activity_saved_seed_phrase_reveal),
    AuthDelegate by AuthDelegateImpl(), GoogleSignInDelegate by GoogleSignInDelegateImpl() {

    private val binding: ActivitySavedSeedPhraseRevealBinding by lazy {
        ActivitySavedSeedPhraseRevealBinding.bind(findViewById(R.id.toastLayoutTopError))
    }
    private lateinit var viewModel: SavedSeedPhraseRevealViewModel
    private lateinit var googleDriveBackupViewModel: GoogleDriveCreateBackupViewModel
    private var backupCreationTime = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get()
        googleDriveBackupViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get()

        setActionBarTitle("")
        hideActionBarBack(isVisible = true)

        initWords()
        initBlur()
        initButtons()

        setupGoogleSignIn()
        subscribeToEvents()
        subscribeToState()
    }

    override fun onResume() {
        super.onResume()
        googleDriveBackupViewModel.checkBackupStatus()
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
    }

    private fun subscribeToEvents() =
        viewModel.eventsFlow.collect(this) { event ->
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
        googleDriveBackupViewModel.backupStatus.collectWhenStarted(this) { status ->
            updateGoogleDriveBackupStatus(status)
        }
        googleDriveBackupViewModel.canCheckBackupStatus.collectWhenStarted(this) { canCheck ->
            if (canCheck) {
                val googleSignInClient = GoogleDriveManager.getSignInClient(this)
                val signInIntent = googleSignInClient.signInIntent
                launchGoogleSignIn(signInIntent)
            }
        }
        googleDriveBackupViewModel.backupFile.collectWhenStarted(this) { file ->
            file?.let {
                backupCreationTime = file.createdTime?.toString()
                    ?.toDate()
                    ?.formatTo("dd MMM yyyy HH:mm:ss") ?: ""
            }
        }
    }

    private fun updateGoogleDriveBackupStatus(status: GoogleDriveCreateBackupViewModel.BackupStatus) {
        when (status) {
            GoogleDriveCreateBackupViewModel.BackupStatus.BackedUp -> {
                binding.googleDriveBackupStatus.text =
                    getString(R.string.settings_overview_google_drive_backup_active)
                binding.googleDriveBackupStatus.setTextColor(getColor(R.color.mw24_green))

                binding.backupButton.setOnClickListener {
                    GoogleDriveDeleteBackupBottomSheet.newInstance(
                        GoogleDriveDeleteBackupBottomSheet.setBundle(backupCreationTime)
                    ).showSingle(
                        supportFragmentManager,
                        GoogleDriveDeleteBackupBottomSheet.TAG
                    )
                }
            }

            GoogleDriveCreateBackupViewModel.BackupStatus.NotBackedUp -> {
                binding.googleDriveBackupStatus.text =
                    getString(R.string.settings_overview_google_drive_backup_not_active)
                binding.googleDriveBackupStatus.setTextColor(getColor(R.color.attention_red))

                binding.backupButton.setOnClickListener {
                    gotoGoogleDriveBackUp()
                }
            }

            GoogleDriveCreateBackupViewModel.BackupStatus.Processing -> {
                binding.googleDriveBackupStatus.text =
                    getString(R.string.settings_overview_google_drive_backup_processing)
                binding.googleDriveBackupStatus.setTextColor(getColor(R.color.mw24_blue_3))
                binding.backupButton.setOnClickListener { }
            }
        }
    }

    private fun setupGoogleSignIn() {
        registerLauncher(
            caller = this,
            onSuccess = googleDriveBackupViewModel::setGoogleSignInAccount,
            onFailure = {}
        )
    }

    override fun loggedOut() {
        // No need to show auth, as it is anyway requested further.
    }

    private fun gotoGoogleDriveBackUp() {
        val intent = Intent(this, GoogleDriveCreateBackupActivity::class.java)
        startActivity(intent)
    }
}
