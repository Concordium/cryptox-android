package com.concordium.wallet.uicore

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.concordium.wallet.R

class ProgressLine : View {
    private var filled = 1
    private var numberOfDots = 4
    private val defaultDotPaint = Paint().apply {
        color = ResourcesCompat.getColor(resources, R.color.cryptox_black_additional, null)
        isAntiAlias = true
        isDither = true
        strokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            1f,
            context.resources.displayMetrics
        )
    }
    private val currentDotPaint = Paint().apply {
        color = ResourcesCompat.getColor(resources, R.color.cryptox_white_main_30, null)
        isAntiAlias = true
        isDither = true
        strokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            1f,
            context.resources.displayMetrics
        )
    }
    private val currentDotInnerPaint = Paint().apply {
        color = ResourcesCompat.getColor(resources, R.color.cryptox_white_main, null)
        isAntiAlias = true
        isDither = true
        style = Paint.Style.FILL
    }
    private val linePaint = Paint().apply {
        color = ResourcesCompat.getColor(resources, R.color.cryptox_black_additional, null)
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            1f,
            context.resources.displayMetrics
        )
    }

    constructor (context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressLine, 0, 0)
            try {
                numberOfDots = typedArray.getInt(R.styleable.ProgressLine_numberOfDots, 4)
                filled = typedArray.getInt(R.styleable.ProgressLine_filledDots, 1)
            } finally {
                typedArray.recycle()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cy = (height / 2).toFloat()
        var cx = cy
        var x = height.toFloat()
        val y = cy
        val currentDotRadius = height.toFloat() / 2
        val defaultDotRadius = currentDotRadius * 0.6f
        val dotSpacing = (width - (height * numberOfDots)).toFloat() / (numberOfDots - 1)

        for (i in 1..numberOfDots) {
            val dotRadius =
                if (i == filled)
                    currentDotRadius
                else
                    defaultDotRadius
            val dotPaint =
                if (i == filled)
                    currentDotPaint
                else
                    defaultDotPaint
            dotPaint.style =
                if (i > filled)
                    Paint.Style.STROKE
                else
                    Paint.Style.FILL

            canvas.drawCircle(cx, cy, dotRadius, dotPaint)
            if (i == filled) {
                canvas.drawCircle(cx, cy, defaultDotRadius, currentDotInnerPaint)
            }

            if (i < numberOfDots) {
                canvas.drawLine(
                    cx + defaultDotRadius,
                    y,
                    x + defaultDotRadius + dotSpacing - dotPaint.strokeWidth,
                    cy,
                    linePaint
                )
            }

            x += dotSpacing + height
            cx = x - cy
        }
    }

    fun setFilledDots(filledDots: Int) {
        filled = filledDots
        invalidate()
    }

    fun setTotalDots(totalDots: Int) {
        numberOfDots = totalDots
        invalidate()
    }
}
