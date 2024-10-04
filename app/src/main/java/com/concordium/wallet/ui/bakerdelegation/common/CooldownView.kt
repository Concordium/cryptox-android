package com.concordium.wallet.ui.bakerdelegation.common

import android.content.Context
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.set
import com.concordium.wallet.R
import com.concordium.wallet.data.model.AccountCooldown
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ViewCooldownBinding
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class CooldownView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ConstraintLayout(context, attrs) {
    private val binding: ViewCooldownBinding

    init {
        LayoutInflater.from(context).inflate(R.layout.view_cooldown, this, true)
        binding = ViewCooldownBinding.bind(this)
    }

    fun setCooldown(cooldown: AccountCooldown) = with(binding) {
        inactiveStakeAmount.text = CurrencyUtil.formatGTU(cooldown.amount)

        val dayLeftCount = (cooldown.timestamp - System.currentTimeMillis())
            .toDuration(DurationUnit.MILLISECONDS)
            .inWholeDays
            // If less than 1 day left, keep showing “1 day left”.
            .coerceAtLeast(1)
            .toInt()
        val daysLeftString = resources.getQuantityString(
            R.plurals.days_left,
            dayLeftCount,
            dayLeftCount
        )
        val dayLeftCountIndex = daysLeftString.indexOf(dayLeftCount.toString())
        inactiveStakeCooldownTimeAmount.text = SpannableString(daysLeftString).apply {
            set(
                dayLeftCountIndex,
                dayLeftCountIndex + dayLeftCount.toString().length,
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        context,
                        R.color.cryptox_white_main
                    )
                )
            )
        }
    }
}
