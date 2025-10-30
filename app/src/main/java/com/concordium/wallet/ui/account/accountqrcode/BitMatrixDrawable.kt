package com.concordium.wallet.ui.account.accountqrcode

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import com.google.zxing.common.BitMatrix

class BitMatrixDrawable(
    private val bitMatrix: BitMatrix,
    @ColorInt
    squareColor: Int = Color.BLACK,
) : Drawable() {
    private val defaultSquareColor = squareColor
    private val squarePaint = Paint().apply {
        color = squareColor

        // Stroke is to compensate for float rounding.
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 1f
    }

    private var squareWidth: Float = 1f
    private var squareHeight: Float = 1f

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        squareWidth = (right - left).toFloat() / bitMatrix.width
        squareHeight = (bottom - top).toFloat() / bitMatrix.height
    }

    override fun draw(canvas: Canvas) {
        (0 until bitMatrix.width).forEach { x ->
            (0 until bitMatrix.height).forEach { y ->
                if (bitMatrix[x, y]) {
                    val left = x * squareWidth
                    val top = y * squareHeight
                    canvas.drawRect(
                        RectF(
                            left,
                            top,
                            left + squareWidth,
                            top + squareHeight
                        ),
                        squarePaint
                    )
                }
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        squarePaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        squarePaint.colorFilter = colorFilter
    }

    override fun setTintList(tint: ColorStateList?) {
        if (tint != null) {
            squarePaint.color = tint.defaultColor
        } else {
            squarePaint.color = defaultSquareColor
        }
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java",
        ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
