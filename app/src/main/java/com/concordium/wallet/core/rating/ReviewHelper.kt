package com.concordium.wallet.core.rating

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

class ReviewHelper(private val activity: Activity) {

    fun launchReviewFlow() {
        val reviewManager = ReviewManagerFactory.create(activity)
        reviewManager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reviewManager.launchReviewFlow(activity, task.result)
            }
        }
    }
}