package com.concordium.wallet.ui.onboarding

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import com.concordium.wallet.databinding.OnboardingStatusCardBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.identity.identityproviderlist.IdentityProviderListActivity
import com.concordium.wallet.ui.seed.setup.OneStepSetupWalletActivity
import com.concordium.wallet.uicore.dialog.UnlockFeatureDialog

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
    private val activity = context as? FragmentActivity

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
    }

    fun updateViews(state: OnboardingState) {
        val currentState = dataProvider.getViewDataByState(state, IdentityVerificationState.IN_PROGRESS)

        binding.onboardingStatusTitle.text = currentState.statusTitle
        binding.onboardingStatusDescription.text = currentState.statusDescription
        binding.onboardingActionButton.text = currentState.actionButtonTitle

        if (currentState.showProgressBar) {
            animateProgressBar(currentState.progressPrevious, currentState.progressCurrent)
        }

        if (currentState.animateStatusIcon) {
            animateStatusIcon(binding.identityVerificationStatusIcon)
        }

        when(state) {
            OnboardingState.INITIAL -> {
                binding.onboardingActionButton.setOnClickListener {
                    goToCreateWallet()
                }
            }
            OnboardingState.SAVE_PHRASE -> {
                binding.onboardingActionButton.setOnClickListener {
                    goToFirstIdentityCreation()
                }
            }
            else -> {}
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

    private fun animateStatusIcon(imageView: ImageView) {
        val pulsateAnimator = ObjectAnimator.ofFloat(imageView, "alpha", 1f, 0f, 1f)
        pulsateAnimator.duration = 1500 // 1.5 second
        pulsateAnimator.repeatCount = ObjectAnimator.INFINITE
        pulsateAnimator.repeatMode = ObjectAnimator.REVERSE

        // Add an interpolator for a smooth pulsating effect (ease-in-out)
        pulsateAnimator.interpolator = AccelerateDecelerateInterpolator()
        pulsateAnimator.start()
    }

    private fun showUnlockFeatureDialog() {
        activity?.let {
            UnlockFeatureDialog().showSingle(
                it.supportFragmentManager,
                UnlockFeatureDialog.TAG
            )
        }
    }

    private fun goToCreateWallet() {
        activity?.startActivity(Intent(activity, OneStepSetupWalletActivity::class.java))
    }

    private fun goToFirstIdentityCreation() {
        val intent = Intent(context, IdentityProviderListActivity::class.java)
        intent.putExtra(IdentityProviderListActivity.SHOW_FOR_FIRST_IDENTITY, true)
        activity?.startActivity(intent)
    }
}