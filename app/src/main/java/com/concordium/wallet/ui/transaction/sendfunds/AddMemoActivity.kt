package com.concordium.wallet.ui.transaction.sendfunds

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import com.concordium.sdk.serializing.CborMapper
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityAddMemoBinding
import com.concordium.wallet.ui.base.BaseActivity

class AddMemoActivity : BaseActivity(R.layout.activity_add_memo, R.string.add_memo_title) {

    companion object {
        const val EXTRA_MEMO = "EXTRA_MEMO"
        const val MAX_MEMO_SIZE_BYTES = 256
    }

    private val binding by lazy {
        ActivityAddMemoBinding.bind(findViewById(R.id.root_layout))
    }
    private var dialog: Dialog? = null
    private var previousText: String = ""

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val memo = intent.getStringExtra(EXTRA_MEMO) ?: ""
        if (memo.isNotEmpty()) {
            binding.memoEdittext.setText(memo)
            binding.memoEdittext.setSelection(memo.length)
            binding.confirmButton.isEnabled = true
            handleDeleteBtn(isVisible = true)
            previousText = memo
        }

        binding.memoEdittext.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(editable: Editable) {
                val str = editable.toString()
                val isNotEmpty = str.trim().isNotEmpty()
                binding.confirmButton.isEnabled = isNotEmpty
                handleDeleteBtn(isVisible = isNotEmpty)
                val bytes = CborMapper.INSTANCE.writeValueAsBytes(str)
                val change = bytes.size <= MAX_MEMO_SIZE_BYTES
                if (!change) {
                    if (previousText.isNotEmpty()) {
                        editable.replace(0, editable.length, previousText)
                    }
                    binding.memoEdittext.startAnimation(
                        AnimationUtils.loadAnimation(
                            this@AddMemoActivity,
                            R.anim.anim_shake
                        )
                    )
                } else {
                    previousText = str
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.memoEdittext.setOnFocusChangeListener { _, hasFocus ->
            binding.memoEdittext.background = if (hasFocus) {
                ContextCompat.getDrawable(this, R.drawable.mw24_input_field_background_active)
            } else {
                ContextCompat.getDrawable(this, R.drawable.mw24_input_field_background_default)
            }
        }

        binding.confirmButton.setOnClickListener {
            val memoText = binding.memoEdittext.text.toString().trim()
            goBackWithMemo(memoText)
        }

        hideActionBarBack(isVisible = true)
    }

    private fun handleDeleteBtn(isVisible: Boolean) {
        hideActionBarDelete(isVisible = isVisible) {
            goBackWithMemo("")
        }
    }

    //endregion

    //region Initialize
    // ************************************************************

    //endregion

    //region Control/UI
    // ************************************************************

    private fun goBackWithMemo(memoText: String) {
        val intent = Intent()
        intent.putExtra(EXTRA_MEMO, memoText)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    //endregion
}
