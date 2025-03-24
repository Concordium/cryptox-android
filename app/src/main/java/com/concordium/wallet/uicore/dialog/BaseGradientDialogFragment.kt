package com.concordium.wallet.uicore.dialog

import androidx.appcompat.app.AppCompatDialogFragment
import com.concordium.wallet.R

abstract class BaseGradientDialogFragment : AppCompatDialogFragment() {

    override fun getTheme(): Int = R.style.CCX_Dialog
}