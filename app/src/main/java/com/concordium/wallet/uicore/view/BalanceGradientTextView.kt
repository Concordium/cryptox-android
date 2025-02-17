package com.concordium.wallet.uicore.view

import android.content.Context
import android.graphics.RadialGradient
import android.graphics.Shader
import android.text.TextPaint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class BalanceGradientTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyGradient(w, h)
    }

    private fun applyGradient(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            val paint: TextPaint = paint
            // Linear gradient at a 45-degree angle
            val shader = RadialGradient(
                0f,
                0f,
                width / 1f,
                intArrayOf(
                    0xFF9EF2EB.toInt(),
                    0xFFEDDABF.toInt(),
                    0xFFA49AE3.toInt(),
                ),
                null,
                Shader.TileMode.CLAMP
            )
            paint.shader = shader
            invalidate()
        }
    }
}