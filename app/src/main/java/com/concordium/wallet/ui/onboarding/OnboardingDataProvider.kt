package com.concordium.wallet.ui.onboarding

import android.content.Context
import com.concordium.wallet.R

class OnboardingDataProvider(val context: Context) {
        val onboardingData = listOf(
            OnboardingStateModel(
                step = 1,
                statusTitle = context.getString(R.string.onboarding_status_card_title),
                statusDescription = context.getString(R.string.onboarding_status_card_description_first),
                progressPrevious = 0,
                progressCurrent = 33,
                actionButtonTitle = context.getString(R.string.onboarding_action_button_save_seed)
            ),
            OnboardingStateModel(
                step = 2,
                statusTitle = context.getString(R.string.onboarding_status_card_title),
                statusDescription = context.getString(R.string.onboarding_status_card_description_second),
                progressPrevious = 33,
                progressCurrent = 66,
                actionButtonTitle = context.getString(R.string.accounts_overview_create_identity)
            ),
            OnboardingStateModel(
                step = 3,
                statusTitle = context.getString(R.string.onboarding_status_card_title),
                statusDescription = context.getString(R.string.onboarding_status_card_description_second),
                progressPrevious = 66,
                progressCurrent = 100,
                actionButtonTitle = context.getString(R.string.accounts_overview_create_identity)
            )
        )
}