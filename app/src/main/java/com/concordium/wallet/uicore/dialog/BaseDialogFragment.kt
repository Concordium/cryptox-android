package com.concordium.wallet.uicore.dialog

import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import com.concordium.wallet.R

abstract class BaseDialogFragment : AppCompatDialogFragment() {

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
}