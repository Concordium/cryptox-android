package com.concordium.wallet.util

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat
import kotlin.math.abs

object GestureDetectorUtil {

    fun getDrawerGestureDetector(
        context: Context,
        onSwipeRight: () -> Unit,
        onSwipeLeft: () -> Unit
    ): GestureDetectorCompat =
        GestureDetectorCompat(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                private val SWIPE_THRESHOLD = 100
                private val SWIPE_VELOCITY_THRESHOLD = 200

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 == null) return false

                    val deltaX = e2.x - e1.x
                    val deltaY = e2.y - e1.y

                    if (abs(deltaX) > abs(deltaY)) {
                        if (deltaX > SWIPE_THRESHOLD && velocityX > SWIPE_VELOCITY_THRESHOLD) {
                            // Swipe right to open drawer
                            onSwipeRight.invoke()
                            return true
                        } else if (deltaX < -SWIPE_THRESHOLD && velocityX < -SWIPE_VELOCITY_THRESHOLD) {
                            // Swipe left to close drawer
                            onSwipeLeft.invoke()
                            return true
                        }
                    }
                    return false
                }
            }
        )

    fun getAccountGestureDetector(
        context: Context,
        onSwipeUp: () -> Unit,
        onSwipeDown: () -> Unit
    ): GestureDetectorCompat =
        GestureDetectorCompat(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                private val SWIPE_THRESHOLD = 100
                private val SWIPE_VELOCITY_THRESHOLD = 100

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 == null) return false
                    val diffY = e2.y - e1.y
                    if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY < 0) {
                            onSwipeUp.invoke()
                        } else {
                            onSwipeDown.invoke()
                        }
                        return true
                    }
                    return false
                }
            }
        )
}