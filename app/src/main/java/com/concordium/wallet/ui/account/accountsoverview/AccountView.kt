package com.concordium.wallet.ui.account.accountsoverview

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ViewAccountBinding
import com.concordium.wallet.util.ImageUtil

class AccountView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private var accountWithIdentity: AccountWithIdentity? = null
    private val binding: ViewAccountBinding
    private val pulsateAnimator: ObjectAnimator

    init {
        LayoutInflater.from(context).inflate(R.layout.view_account, this, true)
        binding = ViewAccountBinding.bind(this)

        pulsateAnimator = ObjectAnimator.ofFloat(
            binding.accountVerificationStatusIcon,
            "alpha",
            1f,
            0f,
            1f
        )
    }

    fun setAccount(accountWithIdentity: AccountWithIdentity) {
        this.accountWithIdentity = accountWithIdentity

        binding.totalTextview.text =
            binding.root.context.getString(
                R.string.amount,
                CurrencyUtil.formatAndRoundGTU(
                    accountWithIdentity.account.balance,
                    roundDecimals = 2
                )
            )
        binding.address.text = accountWithIdentity.account.address

        binding.notice.isVisible = accountWithIdentity.account.isBakerSuspended
                || accountWithIdentity.account.isBakerPrimedForSuspension
                || accountWithIdentity.account.isDelegationBakerSuspended

        binding.accountIcon.setImageDrawable(
            ImageUtil.getIconById(
                context,
                accountWithIdentity.account.iconId
            )
        )
        binding.accountName.text =
            if (accountWithIdentity.account.isInitial())
                context.getString(
                    R.string.view_account_name_initial,
                    accountWithIdentity.account.getAccountName()
                )
            else
                accountWithIdentity.account.getAccountName()

        binding.identityName.text = accountWithIdentity.identity.name
        binding.statusReadOnly.isVisible = accountWithIdentity.account.readOnly

        binding.isEarning.isVisible =
            (accountWithIdentity.account.isDelegating() || accountWithIdentity.account.isBaking())

        when (accountWithIdentity.account.transactionStatus) {
            TransactionStatus.COMMITTED,
            TransactionStatus.RECEIVED -> {
                binding.balanceLayout.visibility = GONE
                binding.accountStatusLayout.visibility = VISIBLE
                animateStatusIcon()
            }

            TransactionStatus.ABSENT -> {}
            else -> {
                binding.balanceLayout.visibility = VISIBLE
                binding.accountStatusLayout.visibility = GONE
                pulsateAnimator.end()
            }
        }

        binding.totalTextview.setTextColor(
            if (accountWithIdentity.account.isActive)
                context.getColor(R.color.mw24_content_accent_secondary)
            else
                context.getColor(R.color.mw24_plain_white_60)
        )

        binding.identityName.setTextColor(getColorStateList(accountWithIdentity.account.isActive))
        binding.settingsButton.imageTintList =
            getColorStateList(accountWithIdentity.account.isActive)

    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        val account = accountWithIdentity?.account
        if (onItemClickListener != null && account != null) {
            binding.root.setOnClickListener {
                onItemClickListener.onCardClicked(account)
            }
        }
    }

    private fun animateStatusIcon() {
        pulsateAnimator.duration = 1500 // 1.5 second
        pulsateAnimator.repeatCount = ObjectAnimator.INFINITE
        pulsateAnimator.repeatMode = ObjectAnimator.REVERSE

        // Add an interpolator for a smooth pulsating effect (ease-in-out)
        pulsateAnimator.interpolator = AccelerateDecelerateInterpolator()
        pulsateAnimator.start()
    }

    interface OnItemClickListener {
        fun onCardClicked(account: Account)
    }

    private fun getColorStateList(isActive: Boolean) = if (isActive) {
        context.getColorStateList(R.color.cryptox_white_main)
    } else {
        context.getColorStateList(R.color.mw24_plain_white_60)
    }
}
