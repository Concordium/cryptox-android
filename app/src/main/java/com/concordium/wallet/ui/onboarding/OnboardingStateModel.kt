package com.concordium.wallet.ui.onboarding

import android.graphics.drawable.Drawable

data class OnboardingStateModel(
    val state: OnboardingState,
    val statusTitle: String,
    val statusTextColor: Int,
    val statusDescription: String,
    val verificationStatusIcon: Drawable? = null,
    val showVerificationStatusIcon: Boolean,
    val identityVerificationStatus: String = "",
    val animateStatusIcon: Boolean,
    val showProgressBar: Boolean = true,
    val progressPrevious: Int = 0,
    val progressCurrent: Int = 0,
    val showActionButton: Boolean,
    val actionButtonTitle: String,
    val showInnerActionButton: Boolean,
    val innerActionButtonTitle: String,
    val innerActionButtonBackground: Int? = null
)