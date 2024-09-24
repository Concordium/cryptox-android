package com.concordium.wallet.ui.account.accountsoverview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.databinding.ViewAccountNameAreaBinding

class AccountItemNameAreaView(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {
    private val binding: ViewAccountNameAreaBinding

    init {
        LayoutInflater.from(context).inflate(R.layout.view_account_name_area, this, true)
        binding = ViewAccountNameAreaBinding.bind(this)
    }

    fun setData(accountWithIdentity: AccountWithIdentity) {
        binding.statusImageview.setImageResource(
            when (accountWithIdentity.account.transactionStatus) {
                TransactionStatus.COMMITTED -> R.drawable.ic_pending_filled
                TransactionStatus.RECEIVED -> R.drawable.ic_pending_filled
                TransactionStatus.ABSENT -> R.drawable.ic_status_problem
                else -> 0
            }
        )
        binding.statusImageview.visibility =
            if (accountWithIdentity.account.transactionStatus == TransactionStatus.FINALIZED) View.GONE else View.VISIBLE

        binding.nameTextview.text =
            if (accountWithIdentity.account.isInitial())
                context.getString(
                    R.string.view_account_name_initial,
                    accountWithIdentity.account.getAccountName()
                )
            else
                accountWithIdentity.account.getAccountName()

        binding.identityTextview.text = accountWithIdentity.identity.name

        binding.statusReadOnly.isVisible = accountWithIdentity.account.readOnly
        binding.statusBaker.isVisible = accountWithIdentity.account.isBaking()
        binding.statusDelegating.isVisible = accountWithIdentity.account.isDelegating()
    }
}
