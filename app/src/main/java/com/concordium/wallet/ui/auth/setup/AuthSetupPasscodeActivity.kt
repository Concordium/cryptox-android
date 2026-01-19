package com.concordium.wallet.ui.auth.setup

import android.content.DialogInterface
import android.content.DialogInterface.OnDismissListener
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isInvisible
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityAuthSetupPasscodeBinding
import com.concordium.wallet.extension.collect
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.auth.setup.password.AuthSetupPasswordActivity
import com.concordium.wallet.ui.base.BaseActivity

class AuthSetupPasscodeActivity :
    BaseActivity(R.layout.activity_auth_setup_passcode),
    OnDismissListener {

    private val binding: ActivityAuthSetupPasscodeBinding by lazy {
        ActivityAuthSetupPasscodeBinding.bind(findViewById(R.id.toastLayoutTopError))
    }
    private val viewModel: AuthSetupPasscodeViewModel by viewModels()

    private val getResultAuthSetupFullPassword =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            this::onAuthSetupPasswordResult
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.isSeedPhraseWalletSetupRequired =
            intent.getBooleanExtra(SET_UP_SEED_PHRASE_WALLET_EXTRA, false)

        hideActionBarBack(isVisible = true)
        setActionBarTitle("")

        initInput()
        initButtons()

        subscribeToState()
        subscribeToEvents()
    }

    override fun onResume() {
        super.onResume()
        App.appCore.tracker.passcodeScreen()
    }

    private fun initInput() = with(binding.passcodeInputView) {
        length = viewModel.passcodeLength
        biometricsButton.isInvisible = true

        mutableInput.observe(this@AuthSetupPasscodeActivity) { inputValue ->
            if (inputValue.length == viewModel.passcodeLength) {
                viewModel.onPasscodeEntered(inputValue)
            }
        }
    }

    private fun initButtons() {
        binding.usePasswordButton.setOnClickListener {
            viewModel.onUseFullPasswordClicked()
        }
    }

    // When dealing with fragment transaction,
    // the Started state must be ensured to avoid state loss errors.
    private fun subscribeToState(
    ) = viewModel.stateFlow.collectWhenStarted(this) { state ->
        binding.titleTextView.text = when (state) {
            is AuthSetupPasscodeViewModel.State.Create -> getString(R.string.passcode_create_title)
            AuthSetupPasscodeViewModel.State.Repeat -> getString(R.string.passcode_repeat_title)
        }

        binding.detailsTextView.text = when (state) {
            is AuthSetupPasscodeViewModel.State.Create -> getString(
                R.string.template_passcode_create_details,
                viewModel.passcodeLength
            )

            AuthSetupPasscodeViewModel.State.Repeat -> getString(
                R.string.template_passcode_repeat_details,
                viewModel.passcodeLength
            )
        }

        when (state) {
            is AuthSetupPasscodeViewModel.State.Create -> {
                binding.passcodeInputView.reset()
                if (state.hasError) {
                    binding.passcodeInputView.animateError()
                }
            }

            AuthSetupPasscodeViewModel.State.Repeat -> {
                binding.passcodeInputView.reset()
            }
        }
    }

    private fun subscribeToEvents(
    ) = viewModel.eventsFlow.collect(this) { event ->
        when (event) {
            AuthSetupPasscodeViewModel.Event.FinishWithSuccess -> {
                setResult(RESULT_OK)
                finish()
            }

            AuthSetupPasscodeViewModel.Event.ShowFatalError -> {
                showError(R.string.passcode_setup_failed)
            }

            AuthSetupPasscodeViewModel.Event.SuggestBiometricsSetup -> {
                AuthSetupBiometricsDialog().show(
                    supportFragmentManager,
                    AuthSetupBiometricsDialog.TAG
                )
            }

            AuthSetupPasscodeViewModel.Event.OpenFullPasswordSetUp ->
                goToAuthSetupPassword()
        }
    }

    // The activity listens for biometrics dialog dismiss this way.
    override fun onDismiss(dialog: DialogInterface?) {
        viewModel.onBiometricsSuggestionReviewed()
    }

    private fun goToAuthSetupPassword() {
        val intent = Intent(this, AuthSetupPasswordActivity::class.java)
        getResultAuthSetupFullPassword.launch(intent)
    }

    private fun onAuthSetupPasswordResult(result: ActivityResult) =
        viewModel.onFullPasswordSetUpResult(
            isSetUpSuccessfully = result.resultCode == RESULT_OK
        )

    override fun loggedOut() {
    }

    companion object {
        /**
         * Whether to also set up a seed phrase wallet
         * on a successful passcode/password setup.
         */
        const val SET_UP_SEED_PHRASE_WALLET_EXTRA = "set_up_seed_phrase_wallet"
    }
}
