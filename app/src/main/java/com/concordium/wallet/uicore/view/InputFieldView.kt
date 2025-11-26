package com.concordium.wallet.uicore.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ViewMw24InputFieldBinding
import com.concordium.wallet.util.KeyboardUtil.showKeyboard

class InputFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewMw24InputFieldBinding =
        ViewMw24InputFieldBinding.inflate(LayoutInflater.from(context), this, true)

    private var iconTextEmpty: Drawable? = null
    private var iconTextFilled: Drawable? = null

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.InputFieldView, 0, 0).apply {
            try {
                binding.label.text = getString(R.styleable.InputFieldView_labelText) ?: "Label"

                iconTextEmpty = getDrawable(R.styleable.InputFieldView_iconTextEmpty)
                iconTextFilled = getDrawable(R.styleable.InputFieldView_iconTextFilled)

                binding.searchIcon.setImageDrawable(iconTextEmpty)
                binding.clearIcon.setImageDrawable(
                    iconTextFilled ?: ContextCompat.getDrawable(
                        context,
                        R.drawable.mw24_ic_clear
                    )
                )

            } finally {
                recycle()
            }
        }

        binding.edittext.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateIconFromText(s)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Initial icon state
        updateIconFromText(binding.edittext.text)

        binding.edittext.setOnFocusChangeListener { _, hasFocus ->
            binding.root.background = if (hasFocus) {
                ContextCompat.getDrawable(context, R.drawable.mw24_input_field_background_active)
            } else {
                ContextCompat.getDrawable(context, R.drawable.mw24_input_field_background_default)
            }
        }

        binding.root.setOnClickListener {
            showKeyboard(context, binding.edittext)
        }

        binding.clearIcon.setOnClickListener { clearSearchText() }
    }

    private fun updateIconFromText(text: CharSequence?) {
        binding.clearIcon.isVisible = !text.isNullOrEmpty()
    }

    private fun clearSearchText() {
        binding.edittext.setText("")
    }

    fun updateSearchIconFromText(text: CharSequence?) {
        binding.searchIcon.isVisible = text.isNullOrEmpty()

    }

    fun setLabelText(text: String) {
        binding.label.text = text
    }

    fun setIconTextEmpty(drawable: Drawable) {
        iconTextEmpty = drawable
        updateIconFromText(binding.edittext.text)
    }

    fun setIconTextFilled(drawable: Drawable) {
        iconTextFilled = drawable
        updateIconFromText(binding.edittext.text)
    }

    fun getText(): String = binding.edittext.text.toString()

    fun setText(text: String) = binding.edittext.setText(text)

    fun setSearchListener(listener: OnClickListener) {
        binding.searchIcon.setOnClickListener(listener)
    }

    fun setClearListener(listener: OnClickListener) {
        binding.clearIcon.setOnClickListener(listener)
    }

    fun setTextChangeListener(textListener: TextWatcher) {
        binding.edittext.addTextChangedListener(textListener)
    }

    fun setInputType(inputType: Int) {
        binding.edittext.inputType = inputType
    }

    fun setOnSearchDoneListener(listener: () -> Unit) {
        binding.edittext.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                listener.invoke()
                true
            } else {
                false
            }
        }
    }
}
