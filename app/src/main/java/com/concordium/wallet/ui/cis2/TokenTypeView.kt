package com.concordium.wallet.ui.cis2

import android.content.Context
import android.content.res.ColorStateList
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.concordium.wallet.R
import com.concordium.wallet.data.model.ContractToken
import com.concordium.wallet.data.model.ProtocolLevelToken
import com.concordium.wallet.data.model.Token

class TokenTypeView(
   private val typeTextView: TextView,
) {
    private val context: Context
        get() = typeTextView.context

    private val protocolLevelTokenTint: ColorStateList? by lazy {
        ContextCompat.getColorStateList(
            context,
            R.color.mw24_content_accent_secondary
        )
    }

    private val contractTokenTint: ColorStateList? by lazy {
        ContextCompat.getColorStateList(
            context,
            R.color.mw24_plain_white_40
        )
    }

    fun showTokenType(token: Token) = with(typeTextView) {
        when (token) {
            is ProtocolLevelToken -> {
                text = context.getString(R.string.token_type_plt)
                setTextColor(context.getColor(R.color.mw24_content_accent_secondary))
                setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.mw24_ic_plt_token,
                    0,
                    0,
                    0
                )
                TextViewCompat.setCompoundDrawableTintList(
                    typeTextView,
                    protocolLevelTokenTint
                )
            }

            is ContractToken -> {
                text = context.getString(R.string.token_type_cis)
                setTextColor(context.getColor(R.color.mw24_plain_white_40))
                setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.mw24_ic_cis2_token,
                    0,
                    0,
                    0
                )
                TextViewCompat.setCompoundDrawableTintList(
                    typeTextView,
                    contractTokenTint
                )
            }

            else -> {
                isVisible = false
            }
        }
    }
}
