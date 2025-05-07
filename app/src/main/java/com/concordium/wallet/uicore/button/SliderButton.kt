package com.concordium.wallet.uicore.button

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.motion.widget.MotionLayout
import com.concordium.wallet.R
import com.concordium.wallet.databinding.Mw24SliderButtonBinding

class SliderButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : MotionLayout(context, attrs) {

    private var onSliderCompleteListener: (() -> Unit)? = null

    private var binding: Mw24SliderButtonBinding = Mw24SliderButtonBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        binding.root.setTransitionListener(object : TransitionListener {
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {
            }

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float
            ) {
                if (progress < 0.99f) {
                    transitionToStart()
                }
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                if (currentId == R.id.end) {
                    onSliderCompleteListener?.invoke()
                }
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {
            }
        })
    }

    fun setOnSliderCompleteListener(listener: () -> Unit) {
        onSliderCompleteListener = listener
    }

    fun setText(buttonText: String) {
        binding.sliderText.text = buttonText
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        binding.motionLayout.isInteractionEnabled = enabled
    }
}
