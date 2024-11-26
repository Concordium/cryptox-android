package com.concordium.wallet.ui.onboarding

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.concordium.wallet.App
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.databinding.FragmentOnboardingBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.account.newaccountsetup.NewAccountSetupViewModel
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.identity.identityproviderlist.IdentityProviderListActivity
import com.concordium.wallet.ui.seed.setup.OneStepSetupWalletActivity
import com.concordium.wallet.uicore.dialog.UnlockFeatureDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OnboardingFragment @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), AuthDelegate by AuthDelegateImpl() {

    private var binding: FragmentOnboardingBinding = FragmentOnboardingBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )
    private val dataProvider = OnboardingDataProvider(context)
    private val activity = context as FragmentActivity

    private val onboardingViewModel = ViewModelProvider(
        activity,
        ViewModelProvider.AndroidViewModelFactory.getInstance(activity.application)
    )[OnboardingSharedViewModel::class.java]

    private val newAccountViewModel = ViewModelProvider(
        activity,
        ViewModelProvider.AndroidViewModelFactory.getInstance(activity.application)
    )[NewAccountSetupViewModel::class.java]

    private val pulsateAnimator = ObjectAnimator.ofFloat(
        binding.identityVerificationStatusIcon,
        "alpha",
        1f,
        0f,
        1f
    )

    init {
        listOf(
            binding.onrampBtn,
            binding.sendFundsBtn,
            binding.addressBtn,
        ).forEach {
            it.setOnClickListener {
                showUnlockFeatureDialog()
            }
        }
        activity.lifecycleScope.launch {
            onboardingViewModel.identityFlow.collect { identity ->
                binding.onboardingInnerActionButton.setOnClickListener {
                    App.appCore.tracker.homeCreateAccountClicked()
                    createFirstAccount(identity)
                }
            }
        }
        newAccountViewModel.showAuthenticationLiveData.observe(activity, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showAuthentication(
                        activity = context as BaseActivity,
                        onAuthenticated = {
                            newAccountViewModel.continueWithPassword(it)
                            activity.lifecycleScope.launch {
                                onboardingViewModel.setShowLoading(true)
                            }
                        }
                    )
                }
            }
        })
        newAccountViewModel.gotoAccountCreatedLiveData.observe(activity, object : EventObserver<Account>() {
            override fun onUnhandledEvent(value: Account) {
                App.appCore.session.hasCompletedOnboarding()
                activity.lifecycleScope.launch {
                    onboardingViewModel.setShowLoading(false)
                    onboardingViewModel.setUpdateState(true)
                }
            }
        })
        newAccountViewModel.errorLiveData.observe(activity, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                activity.lifecycleScope.launch {
                    onboardingViewModel.setShowLoading(false)
                }
                (activity as BaseActivity).showError(value)
            }
        })
    }

    fun updateViewsByState(state: OnboardingState) {
        val currentDataState = dataProvider.getViewDataByState(state)
        updateViews(currentDataState)
        App.appCore.tracker.homeIdentityVerificationStateChanged(currentDataState.state.name)

        when (state) {
            OnboardingState.SAVE_PHRASE -> {
                binding.onboardingActionButton.setOnClickListener {
                    App.appCore.tracker.homeSaveSeedPhraseClicked()
                    goToCreateWallet()
                }
            }

            OnboardingState.VERIFY_IDENTITY -> {
                binding.onboardingActionButton.setOnClickListener {
                    App.appCore.tracker.homeIdentityVerificationClicked()
                    goToFirstIdentityCreation()
                }
            }

            OnboardingState.IDENTITY_UNSUCCESSFUL -> {
                binding.onboardingInnerActionButton.setOnClickListener {
                    App.appCore.tracker.homeIdentityVerificationClicked()
                    goToFirstIdentityCreation()
                }
            }

            else -> {}
        }
    }

    fun updateViewsByIdentityStatus(identity: Identity) {
        val currentDataState = dataProvider.getViewDataByIdentityVerificationStatus(identity.status)
        updateViews(currentDataState)
        App.appCore.tracker.homeIdentityVerificationStateChanged(currentDataState.state.name)

        when (identity.status) {
            IdentityStatus.DONE -> {
                binding.onboardingInnerActionButton.setOnClickListener {
                    App.appCore.tracker.homeCreateAccountClicked()
                    createFirstAccount(identity)
                }
            }

            IdentityStatus.ERROR -> {
                binding.onboardingInnerActionButton.setOnClickListener {
                    App.appCore.tracker.homeIdentityVerificationClicked()
                    goToFirstIdentityCreation()
                }
            }
        }
    }

    // update OnboardingStatus card according to setup state
    private fun updateViews(currentViewState: OnboardingStateModel) {
        binding.onboardingStatusTitle.text = currentViewState.statusTitle
        binding.onboardingStatusTitle.setTextColor(currentViewState.statusTextColor)
        binding.onboardingActionButton.text = currentViewState.actionButtonTitle
        binding.onboardingInnerActionButton.text = currentViewState.innerActionButtonTitle
        binding.identityVerificationStatusIcon.setImageDrawable(currentViewState.verificationStatusIcon)

        if (currentViewState.statusDescription.isNotEmpty()) {
            binding.onboardingStatusDescription.visibility = VISIBLE
            binding.onboardingStatusDescription.text = currentViewState.statusDescription
        } else {
            binding.onboardingStatusDescription.visibility = GONE
        }

        if (currentViewState.showProgressBar) {
            binding.onboardingStatusProgressBar.visibility = VISIBLE
            binding.onboardingInnerActionButton.visibility = GONE
            animateProgressBar(currentViewState.progressPrevious, currentViewState.progressCurrent)
        } else {
            currentViewState.innerActionButtonBackground?.let {
                binding.onboardingInnerActionButton.setBackgroundResource(it)
            }

            activity.lifecycleScope.launch {
                if (onboardingViewModel.animatedButtonFlow.value.not()) {
                    animateProgressBarToButton()
                    onboardingViewModel.setAnimatedButton(true)
                } else {
                    binding.onboardingStatusProgressBar.visibility = GONE
                    binding.onboardingInnerActionButton.visibility = VISIBLE
                }
            }
        }

        if (currentViewState.showVerificationStatusIcon) {
            binding.identityVerificationStatusIcon.visibility = VISIBLE

            if (currentViewState.animateStatusIcon) {
                animateStatusIcon()
            } else {
                pulsateAnimator.end()
            }
        } else {
            binding.identityVerificationStatusIcon.visibility = GONE
        }

        if (currentViewState.showActionButton) {
            binding.onboardingActionButton.visibility = VISIBLE
        } else {
            binding.onboardingActionButton.visibility = GONE
        }
    }

    private fun animateProgressBar(startValue: Int, newValue: Int) {
        val progressAnimator = ObjectAnimator.ofInt(
            binding.onboardingStatusProgressBar,
            "progress",
            startValue,
            newValue
        )
        progressAnimator.duration = 1000 // 1 second

        // Set ease-in and ease-out interpolator
        progressAnimator.interpolator = AccelerateDecelerateInterpolator()
        progressAnimator.start()
    }

    private fun animateStatusIcon() {
        pulsateAnimator.duration = 1500 // 1.5 second
        pulsateAnimator.repeatCount = ObjectAnimator.INFINITE
        pulsateAnimator.repeatMode = ObjectAnimator.REVERSE

        // Add an interpolator for a smooth pulsating effect (ease-in-out)
        pulsateAnimator.interpolator = AccelerateDecelerateInterpolator()
        pulsateAnimator.start()
    }

    private fun animateProgressBarToButton() {
        val fadeOutProgressBar = ObjectAnimator.ofFloat(
            binding.onboardingStatusProgressBar, "alpha", 1f, 0f
        ).apply {
            duration = 200
        }

        val fadeInButton = ObjectAnimator.ofFloat(
            binding.onboardingInnerActionButton, "alpha", 0f, 1f
        ).apply {
            duration = 500
        }

        fadeOutProgressBar.doOnEnd {
            binding.onboardingStatusProgressBar.visibility = View.GONE
        }

        fadeInButton.doOnStart {
            binding.onboardingInnerActionButton.visibility = View.VISIBLE
        }

        AnimatorSet().apply {
            playTogether(fadeOutProgressBar, fadeInButton)
            start()
        }
    }

    private fun showUnlockFeatureDialog() {
        UnlockFeatureDialog().showSingle(
            activity.supportFragmentManager,
            UnlockFeatureDialog.TAG
        )
    }

    private fun goToCreateWallet() {
        activity.startActivity(Intent(activity, OneStepSetupWalletActivity::class.java))
    }

    private fun goToFirstIdentityCreation() {
        val intent = Intent(context, IdentityProviderListActivity::class.java)
        intent.putExtra(IdentityProviderListActivity.SHOW_FOR_FIRST_IDENTITY, true)
        activity.startActivity(intent)
    }

    private fun createFirstAccount(identity: Identity) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            newAccountViewModel.initialize(Account.getDefaultName(""), identity)
            newAccountViewModel.createAccount()
        }
    }
}
