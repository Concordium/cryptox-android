package com.concordium.wallet.uicore.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogBaseBinding

abstract class BaseDialogFragment : AppCompatDialogFragment() {

    protected lateinit var binding: DialogBaseBinding

    override fun getTheme(): Int = R.style.CCX_Dialog

    override fun onStart() {
        super.onStart()

        dialog?.window?.let { window ->
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            window.setGravity(Gravity.BOTTOM)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogBaseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.closeButton.setOnClickListener {
            dismiss()
        }

        binding.okButton.setOnClickListener {
            dismiss()
        }
    }

    protected fun setViews(
        title: String,
        description: String,
        okButtonText: String = "",
        cancelButtonText: String = "",
        @DrawableRes iconResId: Int? = null
    ) {
        with(binding) {
            titleTextView.text = title
            detailsTextView.text = description
            okButton.text = okButtonText
            cancelButton.text = cancelButtonText
            cancelButton.isVisible = cancelButtonText.isNotEmpty()

            if (iconResId != null) {
                dialogIconView.setImageResource(iconResId)
                dialogIconView.isVisible = true
            } else {
                dialogIconView.isVisible = false
            }
        }
    }
}