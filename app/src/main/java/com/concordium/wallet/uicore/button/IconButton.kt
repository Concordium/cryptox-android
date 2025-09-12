package com.concordium.wallet.uicore.button

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.databinding.PrimaryButtonWithIconLayoutBinding

class IconButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = PrimaryButtonWithIconLayoutBinding.inflate(
        LayoutInflater.from(context),
        this,
    )

    private var priority: Int = 1
    private var isInverse: Boolean = false

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.IconButton,
            defStyleAttr,
            0
        ).apply {
            try {
                priority = getInt(R.styleable.IconButton_priority, 1)
                isInverse = getBoolean(R.styleable.IconButton_inverse, false)

                binding.buttonText.text =
                    getString(R.styleable.IconButton_buttonText) ?: "Button"
                binding.buttonText.setTextColor(getTextColorStateList())

                setIconStartDrawable(getDrawable(R.styleable.IconButton_iconStart))
                setIconEndDrawable(getDrawable(R.styleable.IconButton_iconEnd))

                when (priority) {
                    1 -> {
                        binding.root.setBackgroundResource(
                            if (isInverse)
                                R.drawable.mw24_button_primary_inverse_background
                            else
                                R.drawable.mw24_button_primary_background
                        )
                    }

                    2 -> {
                        binding.root.setBackgroundResource(
                            if (isInverse)
                                R.drawable.mw24_button_secondary_inverse_background
                            else
                                R.drawable.mw24_button_secondary_background
                        )
                    }

                    3 -> {
                        binding.root.setBackgroundResource(
                            if (isInverse)
                                R.drawable.mw24_button_tertiary_inverse_background
                            else
                                R.drawable.mw24_button_tertiary_background
                        )
                    }
                }
            } finally {
                recycle()
            }
        }
    }

    fun setIconStartDrawable(drawable: Drawable?) {
        with(binding.iconStart) {
            setImageDrawable(drawable)
            if (drawable != null) {
                imageTintList = getTextColorStateList()
            }
            isVisible = drawable != null
        }
    }

    fun setIconEndDrawable(drawable: Drawable?) {
        with(binding.iconEnd) {
            setImageDrawable(drawable)
            if (drawable != null) {
                imageTintList = getTextColorStateList()
            }
            isVisible = drawable != null
        }
    }

    private fun getTextColorStateList() = when (priority) {
        1 ->
            context.getColorStateList(
                if (isInverse)
                    R.color.mw24_button_primary_inverse_text_color
                else
                    R.color.mw24_button_primary_text_color
            )

        2 ->
            context.getColorStateList(
                if (isInverse)
                    R.color.mw24_button_secondary_inverse_text_color
                else
                    R.color.mw24_button_secondary_text_color
            )

        3 ->
            context.getColorStateList(
                if (isInverse)
                    R.color.mw24_button_tertiary_inverse_text_color
                else
                    R.color.mw24_button_tertiary_text_color
            )

        else ->
            error("Unknown priority $priority")
    }
}
