package com.concordium.wallet.uicore.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.RadialGradient
import android.graphics.Shader
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.widget.EditText
import com.concordium.wallet.R
import com.concordium.wallet.uicore.DecimalTextWatcher
import com.concordium.wallet.uicore.MaxAmountTextWatcher

/**
 * An [EditText] with amount validations.
 *
 * @see decimals
 */
@SuppressLint("AppCompatCustomView")
class AmountEditText : EditText {

    constructor (context: Context) : super(context) {
        init(null)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        init(attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }
    private var isGradientTextColor: Boolean = true

    private var currentDecimalTextWatcher: DecimalTextWatcher? = null
        set(value) {
            removeTextChangedListener(field)
            field = value
            addTextChangedListener(value)
        }

    /**
     * Current number of the max allowed decimals.
     * The current entered text must be edited manually.
     */
    var decimals: Int
        get() = currentDecimalTextWatcher?.maxNumberOfDecimals ?: Int.MAX_VALUE
        set(value) {
            currentDecimalTextWatcher = DecimalTextWatcher(value)
        }

    @Suppress("UNUSED_PARAMETER")
    private fun init(attrs: AttributeSet?) {
        inputType = InputType.TYPE_CLASS_NUMBER
        keyListener = DigitsKeyListener.getInstance("0123456789.,")
        decimals = 6

        addTextChangedListener(MaxAmountTextWatcher())

        context.theme.obtainStyledAttributes(attrs, R.styleable.AmountEditText, 0, 0).apply {
            isGradientTextColor = getBoolean(R.styleable.AmountEditText_isGradientTextColor, true)
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        val radius = dpToPx(context, 250f)

        val gradient = RadialGradient(
            0f, 0f, radius,
            intArrayOf(
                0xE69EF2EB.toInt(),
                0xE6EDDABF.toInt(),
                0xE6A49AE3.toInt()
            ),
            null,
            Shader.TileMode.CLAMP
        )

        if (isGradientTextColor) {
            paint.shader = gradient
        }
        super.onDraw(canvas)
    }

    private fun dpToPx(context: Context, dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
}
