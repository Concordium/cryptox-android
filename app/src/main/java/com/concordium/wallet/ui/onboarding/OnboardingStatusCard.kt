package com.concordium.wallet.ui.onboarding

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.databinding.OnboardingStatusCardBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.identity.identityconfirmed.IdentityConfirmedActivity
import com.concordium.wallet.ui.identity.identityproviderlist.IdentityProviderListActivity
import com.concordium.wallet.ui.seed.setup.OneStepSetupWalletActivity
import com.concordium.wallet.uicore.dialog.UnlockFeatureDialog
import kotlinx.coroutines.launch

class OnboardingStatusCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var binding: OnboardingStatusCardBinding = OnboardingStatusCardBinding.inflate(
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
                    createFirstAccount(identity)
                }
            }
        }
    }

    fun updateViewsByState(state: OnboardingState) {
        val currentDataState = dataProvider.getViewDataByState(state)
        println("OnboardingStatusCard, updateViewsByState: ${state.name}")
        updateViews(currentDataState)

        when (state) {
            OnboardingState.SAVE_PHRASE -> {
                binding.onboardingActionButton.setOnClickListener {
                    goToCreateWallet()
                }
            }

            OnboardingState.VERIFY_IDENTITY -> {
                binding.onboardingActionButton.setOnClickListener {
                    goToFirstIdentityCreation()
                }
            }

            OnboardingState.IDENTITY_UNSUCCESSFUL -> {
                binding.onboardingInnerActionButton.setOnClickListener {
                    goToFirstIdentityCreation()
                }
            }

            else -> {}
        }
    }

    fun updateViewsByIdentityStatus(identity: Identity) {
        val currentDataState = dataProvider.getViewDataByIdentityVerificationStatus(identity.status)
        updateViews(currentDataState)

        when (identity.status) {
            IdentityStatus.DONE -> {
                binding.onboardingInnerActionButton.setOnClickListener {
                    createFirstAccount(identity)
                }
            }

            IdentityStatus.ERROR -> {
                binding.onboardingInnerActionButton.setOnClickListener {
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
            binding.onboardingStatusProgressBar.visibility = GONE
            binding.onboardingInnerActionButton.visibility = VISIBLE

            currentViewState.innerActionButtonBackground?.let {
                binding.onboardingInnerActionButton.setBackgroundResource(it)
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
        val intent = Intent(activity, IdentityConfirmedActivity::class.java)
        intent.putExtra(IdentityConfirmedActivity.EXTRA_IDENTITY, identity)
        intent.putExtra(IdentityConfirmedActivity.SHOW_FOR_CREATE_ACCOUNT, true)
        activity.startActivity(intent)
    }
}