package com.concordium.wallet.ui.transaction.sendfunds

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.AnimationUtils
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityAddMemoBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.CBORUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddMemoActivity : BaseActivity(R.layout.activity_add_memo, R.string.add_memo_title) {

    companion object {
        const val EXTRA_MEMO = "EXTRA_MEMO"
    }

    private val binding by  lazy {
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
                val bytes = CBORUtil.encodeCBOR(str)
                val change = bytes.size <= CBORUtil.MAX_BYTES
                if (!change) {
                    if (previousText.isNotEmpty()) {
                        editable.replace(0, editable.length, previousText)
                    }
                    binding.memoEdittext.startAnimation(AnimationUtils.loadAnimation(this@AddMemoActivity, R.anim.anim_shake))
                } else {
                    previousText = str
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        binding.confirmButton.setOnClickListener {
            val memoText = binding.memoEdittext.text.toString().trim()
            binding.memoEdittext.setText(memoText)
            if (memoText.isEmpty()) {
                val builder = MaterialAlertDialogBuilder(this)
                builder.setTitle(getString(R.string.transaction_memo_warning_title))
                builder.setMessage(getString(R.string.transaction_memo_empty_warning_text))
                builder.setNegativeButton(getString(R.string.dialog_cancel)) { _, _ ->
                    dialog?.dismiss()
                }
                builder.setPositiveButton(getString(R.string.transaction_memo_warning_continue)) { _, _ ->
                    goBackWithMemo(memoText)
                }
                builder.setCancelable(true)
                dialog = builder.create()
                dialog?.show()
            } else {
                goBackWithMemo(memoText)
            }
        }

        hideActionBarBack(isVisible = true)
    }

    private fun handleDeleteBtn(isVisible: Boolean) {
        hideActionBarDelete(isVisible = isVisible) {
            binding.memoEdittext.setText("")
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
