package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogMemoNoticeBinding

class MemoNoticeDialog : AppCompatDialogFragment() {

    override fun getTheme(): Int = R.style.CCX_Dialog

    private lateinit var binding: DialogMemoNoticeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogMemoNoticeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listOf(binding.showButton, binding.closeButton).forEach {
            it.setOnClickListener {
                setFragmentResult(
                    ACTION_REQUEST,
                    getResultBundle(showAgain = true)
                )
                dismiss()
            }
        }

        binding.hideButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                getResultBundle(showAgain = false)
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "memo_notice"
        const val ACTION_REQUEST = "memo_notice_action"
        private const val SHOW_AGAIN = "show_notice_again"

        fun getResult(bundle: Bundle) = bundle.getBoolean(SHOW_AGAIN, true)

        private fun getResultBundle(showAgain: Boolean) = Bundle().apply {
            putBoolean(SHOW_AGAIN, showAgain)
        }
    }
}