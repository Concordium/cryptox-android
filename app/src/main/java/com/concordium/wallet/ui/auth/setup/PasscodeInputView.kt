package com.concordium.wallet.ui.auth.setup

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.core.view.forEachIndexed
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.R
import com.concordium.wallet.databinding.CcxPasscodeInputLayoutBinding

/**
 * A passcode keyboard with dots.
 *
 * @see length
 * @see inputValue
 */
class PasscodeInputView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ConstraintLayout(context, attrs) {
    private val binding: CcxPasscodeInputLayoutBinding

    val mutableInput: MutableLiveData<String> = MutableLiveData("")
    var inputValue: String
        get() = mutableInput.value ?: ""
        set(value) {
            mutableInput.value = value
        }
    var length: Int = 6
        set(value) {
            field = value
            initDots()
        }
    val biometricsButton: AppCompatImageButton
        get() = binding.biometricsButton

    init {
        LayoutInflater.from(context).inflate(R.layout.ccx_passcode_input_layout, this, true)
        binding = CcxPasscodeInputLayoutBinding.bind(this)

        initDots()
        initButtons()

        if (isInEditMode) {
            inputValue = "123"
            binding.dotsLayout.background = null
        }
    }

    private fun initDots() {
        val dotSize = context.resources.getDimensionPixelSize(R.dimen.ccx_passcode_dot_size)

        binding.dotsLayout.removeAllViews()
        repeat(length) {
            val dotView = View(context).apply {
                layoutParams = LayoutParams(dotSize, dotSize).apply {
                    marginStart = dotSize
                    marginEnd = dotSize
                }
                background = ContextCompat.getDrawable(
                    context,
                    R.drawable.ccx_passcode_dot_background
                )
            }
            binding.dotsLayout.addView(dotView)
        }

        mutableInput.observeForever { currentInput ->
            binding.dotsLayout.forEachIndexed { dotIndex, dotView ->
                dotView.isEnabled = dotIndex < currentInput.length
            }
        }
    }

    private fun initButtons() {
        val numberButtonClickListener = OnClickListener { numberButton ->
            if (inputValue.length < length) {
                inputValue += numberButton.tag.toString().toInt()
            }
        }

        // Keyboard buttons.
        binding.buttonsLayout.forEach { childView ->
            when (childView.id) {
                // Set up backspace button.
                R.id.backspace_button -> with(childView) {
                    setOnClickListener {
                        if (inputValue.isNotEmpty()) {
                            mutableInput.value = inputValue.substring(0, inputValue.length - 1)
                        }
                    }
                }

                else -> with(childView) {
                    // Set up digit button, which has its digit in the tag.
                    if (this is Button) {
                        text = childView.tag.toString()
                        setOnClickListener(numberButtonClickListener)
                    }
                }
            }
        }
    }

    fun animateError() = with(binding.dotsLayout) {
        clearAnimation()
        startAnimation(
            AnimationUtils.loadAnimation(
                context,
                R.anim.anim_shake
            )
        )
    }

    fun reset() {
        inputValue = ""
    }
}
