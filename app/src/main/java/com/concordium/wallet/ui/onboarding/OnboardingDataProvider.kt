package com.concordium.wallet.ui.onboarding

import android.annotation.SuppressLint
import android.content.Context
import com.concordium.wallet.R
import com.concordium.wallet.data.model.IdentityStatus

@SuppressLint("UseCompatLoadingForDrawables")
class OnboardingDataProvider(val context: Context) {
    private val onboardingData = listOf(
        OnboardingStateModel(
            state = OnboardingState.SAVE_PHRASE,
            statusTitle = context.getString(R.string.onboarding_status_card_title),
            statusTextColor = context.getColor(R.color.cryptox_grey_main),
            statusDescription = context.getString(R.string.onboarding_status_card_description_first),
            showVerificationStatusIcon = false,
            animateStatusIcon = false,
            progressPrevious = 0,
            progressCurrent = 33,
            showActionButton = true,
            actionButtonTitle = context.getString(R.string.onboarding_action_button_save_seed)
        ),
        OnboardingStateModel(
            state = OnboardingState.VERIFY_IDENTITY,
            statusTitle = context.getString(R.string.onboarding_status_card_title),
            statusTextColor = context.getColor(R.color.cryptox_grey_main),
            statusDescription = context.getString(R.string.onboarding_status_card_description_second),
            showVerificationStatusIcon = false,
            animateStatusIcon = false,
            progressPrevious = 33,
            progressCurrent = 66,
            showActionButton = true,
            actionButtonTitle = context.getString(R.string.accounts_overview_create_identity)
        ),
        OnboardingStateModel(
            state = OnboardingState.IDENTITY_IN_PROGRESS,
            statusTitle = context.getString(R.string.onboarding_identity_verification_in_progress),
            statusTextColor = context.getColor(R.color.yellow),
            statusDescription = context.getString(R.string.onboarding_status_card_description_complete),
            verificationStatusIcon = context.getDrawable(R.drawable.ccx_verification_in_progress),
            showVerificationStatusIcon = true,
            identityVerificationStatus = IdentityStatus.PENDING,
            animateStatusIcon = true,
            progressPrevious = 66,
            progressCurrent = 100,
            showActionButton = true,
            actionButtonTitle = context.getString(R.string.accounts_overview_create_identity)
        ),
        OnboardingStateModel(
            state = OnboardingState.IDENTITY_UNSUCCESSFUL,
            statusTitle = context.getString(R.string.onboarding_identity_verification_unsuccessful),
            statusTextColor = context.getColor(R.color.attention_red),
            statusDescription = "",
            verificationStatusIcon = context.getDrawable(R.drawable.ccx_verification_unsuccessful),
            showVerificationStatusIcon = true,
            identityVerificationStatus = IdentityStatus.ERROR,
            animateStatusIcon = false,
            showProgressBar = false,
            showActionButton = true,
            actionButtonTitle = context.getString(R.string.accounts_overview_create_identity)
        ),
        OnboardingStateModel(
            state = OnboardingState.CREATE_ACCOUNT,
            statusTitle = context.getString(R.string.onboarding_identity_verification_completed),
            statusTextColor = context.getColor(R.color.cryptox_green_main),
            statusDescription = "",
            verificationStatusIcon = context.getDrawable(R.drawable.ccx_verification_completed),
            showVerificationStatusIcon = true,
            identityVerificationStatus = IdentityStatus.DONE,
            animateStatusIcon = false,
            showProgressBar = false,
            showActionButton = true,
            actionButtonTitle = context.getString(R.string.accounts_overview_create_account)
        ),
        OnboardingStateModel(
            state = OnboardingState.FINALIZING_ACCOUNT,
            statusTitle = context.getString(R.string.onboarding_finalizing_account),
            statusTextColor = context.getColor(R.color.cryptox_green_main),
            statusDescription = "",
            verificationStatusIcon = context.getDrawable(R.drawable.ccx_finalizing_account),
            showVerificationStatusIcon = true,
            identityVerificationStatus = IdentityStatus.DONE,
            animateStatusIcon = true,
            showProgressBar = false,
            showActionButton = true,
            actionButtonTitle = context.getString(R.string.accounts_overview_create_account)
        )
    )

    fun getViewDataByState(state: OnboardingState) =
        onboardingData.find { it.state == state } ?: OnboardingStateModel(
            state = OnboardingState.SAVE_PHRASE,
            statusTitle = context.getString(R.string.onboarding_status_card_title),
            statusTextColor = context.getColor(R.color.cryptox_grey_main),
            statusDescription = context.getString(R.string.onboarding_status_card_description_first),
            showVerificationStatusIcon = false,
            animateStatusIcon = false,
            progressPrevious = 0,
            progressCurrent = 33,
            showActionButton = true,
            actionButtonTitle = context.getString(R.string.onboarding_action_button_save_seed)
        )

    fun getViewDataByIdentityVerificationStatus(status: String) =
        onboardingData.find { it.identityVerificationStatus == status } ?: OnboardingStateModel(
            state = OnboardingState.IDENTITY_IN_PROGRESS,
            statusTitle = context.getString(R.string.onboarding_identity_verification_in_progress),
            statusTextColor = context.getColor(R.color.yellow),
            statusDescription = context.getString(R.string.onboarding_status_card_description_complete),
            verificationStatusIcon = context.getDrawable(R.drawable.ccx_verification_in_progress),
            showVerificationStatusIcon = true,
            identityVerificationStatus = IdentityStatus.PENDING,
            animateStatusIcon = true,
            showProgressBar = true,
            progressPrevious = 66,
            progressCurrent = 100,
            showActionButton = false,
            actionButtonTitle = ""
        )
}