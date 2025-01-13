package com.concordium.wallet.uicore.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ViewMw24InputFieldBinding

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

                binding.searchIcon.setImageDrawable(iconTextEmpty ?: ContextCompat.getDrawable(context, R.drawable.cryptox_ico_search))


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

        binding.clearIcon.setOnClickListener { binding.edittext.setText("") }
    }

    private fun updateIconFromText(text: CharSequence?) {
        if (text.isNullOrEmpty()) {
            binding.searchIcon.setImageDrawable(iconTextEmpty ?: ContextCompat.getDrawable(context, R.drawable.cryptox_ico_search))
            binding.clearIcon.visibility = View.GONE
            binding.searchIcon.visibility = View.VISIBLE
        } else {
            binding.clearIcon.setImageDrawable(iconTextFilled ?: ContextCompat.getDrawable(context, R.drawable.mw24_ic_clear))
            binding.searchIcon.visibility = View.GONE
            binding.clearIcon.visibility = View.VISIBLE
        }
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

    fun getSearchText(): String = binding.edittext.text.toString()

}
