package com.concordium.wallet.uicore.view

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.concordium.wallet.R

class ThemedCircularProgressDrawable(
    context: Context,
    @ColorInt
    color: Int = ContextCompat.getColor(context, R.color.cryptox_white_main),
    autoStart: Boolean = true
) : CircularProgressDrawable(context) {
    init {
        setColorSchemeColors(color)

        strokeWidth = 4 /*dp*/ * context.resources.displayMetrics.density

        if (autoStart) {
            start()
        }
    }
}
