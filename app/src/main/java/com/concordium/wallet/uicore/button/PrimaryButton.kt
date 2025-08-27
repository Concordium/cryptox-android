package com.concordium.wallet.uicore.button

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.databinding.PrimaryButtonWithIconLayoutBinding

class PrimaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val binding: PrimaryButtonWithIconLayoutBinding

    private var iconStart: Drawable? = null
    private var iconEnd: Drawable? = null
    private var isInverse: Boolean = false

    init {
        LayoutInflater.from(context).inflate(R.layout.primary_button_with_icon_layout, this, true)
        binding = PrimaryButtonWithIconLayoutBinding.bind(this)

        context.theme.obtainStyledAttributes(attrs, R.styleable.PrimaryButton, 0, 0).apply {
            try {
                isInverse = getBoolean(R.styleable.PrimaryButton_inverse, false)

                binding.buttonText.text =
                    getString(R.styleable.PrimaryButton_buttonText) ?: "Button"

                setIconStartDrawable(getDrawable(R.styleable.PrimaryButton_iconStart))
                setIconEndDrawable(getDrawable(R.styleable.PrimaryButton_iconEnd))

                if (isInverse) {
                    binding.buttonLayout.setBackgroundResource(R.drawable.mw24_button_primary_inverse_background)
                    binding.buttonText.setTextColor(
                        context.getColorStateList(R.color.mw24_button_primary_inverse_text_color)
                    )
                } else {
                    binding.buttonLayout.setBackgroundResource(R.drawable.mw24_button_primary_background)
                    binding.buttonText.setTextColor(
                        context.getColorStateList(R.color.mw24_button_primary_text_color)
                    )
                }
            } finally {
                recycle()
            }
        }
    }

    fun setIconStartDrawable(drawable: Drawable?) {
        iconStart = drawable
        with(binding.iconStart) {
            setImageDrawable(iconStart)
            imageTintList = if (isInverse) {
                context.getColorStateList(R.color.mw24_button_primary_inverse_text_color)
            } else {
                context.getColorStateList(R.color.mw24_button_primary_text_color)
            }
            isVisible = iconStart != null
        }
    }

    fun setIconEndDrawable(drawable: Drawable?) {
        iconEnd = drawable
        with(binding.iconEnd) {
            setImageDrawable(iconEnd)
            imageTintList = if (isInverse) {
                context.getColorStateList(R.color.mw24_button_primary_inverse_text_color)
            } else {
                context.getColorStateList(R.color.mw24_button_primary_text_color)
            }
            isVisible = iconEnd != null
        }
    }

    fun setClickListener(listener: OnClickListener) {
        binding.buttonLayout.setOnClickListener(listener)
    }
}