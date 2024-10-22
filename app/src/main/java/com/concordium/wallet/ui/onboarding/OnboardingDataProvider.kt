package com.concordium.wallet.ui.onboarding

import android.annotation.SuppressLint
import android.content.Context
import com.concordium.wallet.R

class OnboardingDataProvider(val context: Context) {
    private val onboardingData = listOf(
        OnboardingStateModel(
            state = OnboardingState.INITIAL,
            statusTitle = context.getString(R.string.onboarding_status_card_title),
            statusTextColor = context.getColor(R.color.cryptox_grey_main),
            statusDescription = context.getString(R.string.onboarding_status_card_description_first),
            showVerificationStatusIcon = false,
            animateStatusIcon = false,
            progressPrevious = 0,
            progressCurrent = 33,
            showActionButton = true,
            actionButtonTitle = context.getString(R.string.onboarding_action_button_save_seed),
            showInnerActionButton = false,
            innerActionButtonTitle = ""
        ),
        OnboardingStateModel(
            state = OnboardingState.SAVE_PHRASE,
            statusTitle = context.getString(R.string.onboarding_status_card_title),
            statusTextColor = context.getColor(R.color.cryptox_grey_main),
            statusDescription = context.getString(R.string.onboarding_status_card_description_second),
            showVerificationStatusIcon = false,
            animateStatusIcon = false,
            progressPrevious = 33,
            progressCurrent = 66,
            showActionButton = true,
            actionButtonTitle = context.getString(R.string.accounts_overview_create_identity),
            showInnerActionButton = false,
            innerActionButtonTitle = ""
        )
    )

    @SuppressLint("UseCompatLoadingForDrawables")
    fun getViewDataByState(
        state: OnboardingState,
        identityVerificationState: IdentityVerificationState? = null
    ): OnboardingStateModel {
        return when (state) {
            OnboardingState.INITIAL -> onboardingData[0]
            OnboardingState.SAVE_PHRASE -> onboardingData[1]
            OnboardingState.VERIFY_IDENTITY -> {
                when (identityVerificationState) {
                    IdentityVerificationState.IN_PROGRESS -> {
                        OnboardingStateModel(
                            state = OnboardingState.VERIFY_IDENTITY,
                            statusTitle = context.getString(R.string.onboarding_identity_verification_in_progress),
                            statusTextColor = context.getColor(R.color.yellow),
                            statusDescription = context.getString(R.string.onboarding_status_card_description_complete),
                            verificationStatusIcon = context.getDrawable(R.drawable.ccx_verification_in_progress),
                            showVerificationStatusIcon = true,
                            animateStatusIcon = true,
                            progressPrevious = 66,
                            progressCurrent = 100,
                            showActionButton = false,
                            actionButtonTitle = "",
                            showInnerActionButton = false,
                            innerActionButtonTitle = ""
                        )
                    }

                    IdentityVerificationState.UNSUCCESSFUL -> {
                        OnboardingStateModel(
                            state = OnboardingState.VERIFY_IDENTITY,
                            statusTitle = context.getString(R.string.onboarding_identity_verification_unsuccessful),
                            statusTextColor = context.getColor(R.color.attention_red),
                            statusDescription = "",
                            verificationStatusIcon = context.getDrawable(R.drawable.ccx_verification_unsuccessful),
                            showVerificationStatusIcon = true,
                            animateStatusIcon = false,
                            showProgressBar = false,
                            showActionButton = false,
                            actionButtonTitle = "",
                            showInnerActionButton = true,
                            innerActionButtonTitle = context.getString(R.string.accounts_overview_create_identity)
                        )
                    }

                    IdentityVerificationState.COMPLETED -> {
                        OnboardingStateModel(
                            state = OnboardingState.VERIFY_IDENTITY,
                            statusTitle = context.getString(R.string.onboarding_identity_verification_completed),
                            statusTextColor = context.getColor(R.color.cryptox_green_main),
                            statusDescription = "",
                            verificationStatusIcon = context.getDrawable(R.drawable.ccx_verification_completed),
                            showVerificationStatusIcon = true,
                            animateStatusIcon = false,
                            showProgressBar = false,
                            showActionButton = false,
                            actionButtonTitle = "",
                            showInnerActionButton = true,
                            innerActionButtonTitle = context.getString(R.string.accounts_overview_create_account)
                        )
                    }

                    else -> {
                        OnboardingStateModel(
                            state = OnboardingState.INITIAL,
                            statusTitle = context.getString(R.string.onboarding_status_card_title),
                            statusTextColor = context.getColor(R.color.cryptox_grey_main),
                            statusDescription = context.getString(R.string.onboarding_status_card_description_first),
                            showVerificationStatusIcon = false,
                            animateStatusIcon = false,
                            progressPrevious = 0,
                            progressCurrent = 33,
                            showActionButton = true,
                            actionButtonTitle = context.getString(R.string.onboarding_action_button_save_seed),
                            showInnerActionButton = false,
                            innerActionButtonTitle = ""
                        )
                    }
                }
            }

            OnboardingState.DONE -> {
                OnboardingStateModel(
                    state = OnboardingState.INITIAL,
                    statusTitle = context.getString(R.string.onboarding_status_card_title),
                    statusTextColor = context.getColor(R.color.cryptox_grey_main),
                    statusDescription = context.getString(R.string.onboarding_status_card_description_first),
                    showVerificationStatusIcon = false,
                    animateStatusIcon = false,
                    progressPrevious = 0,
                    progressCurrent = 33,
                    showActionButton = true,
                    actionButtonTitle = context.getString(R.string.onboarding_action_button_save_seed),
                    showInnerActionButton = false,
                    innerActionButtonTitle = ""
                )
            }
        }
    }
}