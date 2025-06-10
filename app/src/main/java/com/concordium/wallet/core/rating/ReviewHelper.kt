package com.concordium.wallet.core.rating

import android.app.Activity
import com.concordium.wallet.App
import com.google.android.play.core.review.ReviewManagerFactory

class ReviewHelper(private val activity: Activity) {

    fun launchReviewFlow() {
        App.appCore.tracker.reviewAppDialog()

        val reviewManager = ReviewManagerFactory.create(activity)
        reviewManager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reviewManager.launchReviewFlow(activity, task.result).addOnCompleteListener {
                    if (it.isSuccessful) {
                        App.appCore.tracker.reviewAppDialogSubmitClicked()
                    } else {
                        App.appCore.tracker.reviewAppDialogCancelClicked()
                    }
                }
            }
        }
    }
}