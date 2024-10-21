package com.concordium.wallet.ui.onboarding

data class OnboardingStateModel(
    val step: Int,
    val statusTitle: String,
    val statusDescription: String,
    val progressPrevious: Int,
    val progressCurrent: Int,
    val actionButtonTitle: String
)