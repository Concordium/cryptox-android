package com.concordium.wallet.uicore

import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.view.View

class GradientAnimator {

    fun applyGradientAnimation(view: View, duration: Long = 4000) {
        // Define the gradient colors
        val colors = intArrayOf(
            0xE59EF2EB.toInt(),
            0xE5EDDABF.toInt(),
            0xE5A49AE3.toInt()
        )

        // Create a GradientDrawable with a radial gradient
        val gradientDrawable = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            setGradientCenter(0f, 0f) // Center the gradient
            gradientRadius = 1000f // Radius of the gradient
        }
        view.background = gradientDrawable

        // Create an animator to cycle through gradient colors
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE

            addUpdateListener {
                val fraction = it.animatedValue as Float
                val blendedColors = blendColorsArray(colors, fraction)
                gradientDrawable.colors = blendedColors
            }
        }

        animator.start()
    }

    private fun blendColorsArray(colors: IntArray, fraction: Float): IntArray {
        val blendedColors = mutableListOf<Int>()
        for (i in 0 until colors.size - 1) {
            blendedColors.add(blendColors(colors[i], colors[i + 1], fraction))
        }
        blendedColors.add(colors.last()) // Add the last color to complete the array
        return blendedColors.toIntArray()
    }

    private fun blendColors(color1: Int, color2: Int, fraction: Float): Int {
        val red = (1 - fraction) * (color1 shr 16 and 0xFF) + fraction * (color2 shr 16 and 0xFF)
        val green = (1 - fraction) * (color1 shr 8 and 0xFF) + fraction * (color2 shr 8 and 0xFF)
        val blue = (1 - fraction) * (color1 and 0xFF) + fraction * (color2 and 0xFF)
        return (0xFF shl 24) or
                ((red.toInt() and 0xFF) shl 16) or
                ((green.toInt() and 0xFF) shl 8) or
                (blue.toInt() and 0xFF)
    }
}