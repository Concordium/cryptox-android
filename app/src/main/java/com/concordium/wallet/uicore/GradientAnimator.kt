package com.concordium.wallet.uicore

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class GradientAnimator {

    fun applyGradientAnimation(context: Context, view: View, duration: Long = 8000) {
        // Gradient colors
        val colors = intArrayOf(
            0xE69EF2EB.toInt(),
            0xE6EDDABF.toInt(),
            0xE6A49AE3.toInt()
        )

        // GradientDrawable with a radial gradient
        val gradientDrawable = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            setGradientCenter(0f, 0f)
            gradientRadius = dpToPx(context, 800f)
            cornerRadius = dpToPx(context, 16f)
        }
        view.background = gradientDrawable

        // Animator to cycle through gradient colors
        val animator = ValueAnimator.ofFloat(0f, 360f).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE

            addUpdateListener { animation ->
                val angle = (animation.animatedValue as Float) * (PI / 180).toFloat() // Convert to radians
                val radius = 0.3f // The radius of the circle as a fraction of the view's dimensions
                val centerX = 0.5f + radius * cos(angle)
                val centerY = 0.5f + radius * sin(angle)
                gradientDrawable.setGradientCenter(centerX, centerY)

                val fraction = animation.animatedFraction
                gradientDrawable.colors = blendColorsArray(colors, fraction)
            }
        }

        animator.start()
    }

    private fun blendColorsArray(colors: IntArray, fraction: Float): IntArray {
        val blendedColors = mutableListOf<Int>()
        for (i in 0 until colors.size - 1) {
            blendedColors.add(blendColors(colors[i], colors[i + 1], fraction))
        }
        blendedColors.add(colors.last())
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

    private fun dpToPx(context: Context, dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
}