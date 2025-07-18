package com.concordium.wallet.ui.plt

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.data.model.PLTToken
import com.concordium.wallet.databinding.ViewPltAllowDenyListBinding

class PLTAllowDenyListView(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {
    private val binding: ViewPltAllowDenyListBinding
    private var isPLTLabel: Boolean = false

    init {
        LayoutInflater.from(context).inflate(R.layout.view_plt_allow_deny_list, this, true)
        binding = ViewPltAllowDenyListBinding.bind(this)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.PLTAllowDenyListView,
            0,
            0
        ).apply {
            isPLTLabel = getBoolean(R.styleable.PLTAllowDenyListView_isPLTLabel, false)
        }
    }

    fun setToken(token: PLTToken) {
        val pltListStatus = PLTListStatus.ON_DENY_LIST
        when (pltListStatus) {
            PLTListStatus.ON_ALLOW_LIST -> {
                binding.rootLayout.background = AppCompatResources.getDrawable(
                    context,
                    R.drawable.plt_in_allow_list_background
                )
                binding.statusTitle.text =
                    context.getString(R.string.account_details_account_on_allow_list)
                binding.statusTitle.setTextColor(context.getColor(R.color.mw24_content_success_primary))
                binding.icon.setImageResource(R.drawable.mw24_ic_circled_check_done)
            }

            PLTListStatus.NOT_ON_ALLOW_LIST -> {
                binding.rootLayout.background = AppCompatResources.getDrawable(
                    context,
                    R.drawable.plt_not_in_allow_list_background
                )
                binding.statusTitle.text =
                    context.getString(R.string.account_details_account_not_on_allow_list)
                binding.statusTitle.setTextColor(context.getColor(R.color.mw24_content_warning_primary))
                binding.icon.setImageResource(R.drawable.mw24_ic_circled_block_warning)
            }

            PLTListStatus.ON_DENY_LIST -> {
                binding.rootLayout.background = AppCompatResources.getDrawable(
                    context,
                    R.drawable.plt_in_deny_list_background
                )
                binding.statusTitle.text =
                    context.getString(R.string.account_details_account_on_deny_list)
                binding.statusTitle.setTextColor(context.getColor(R.color.mw24_content_error_primary))
                binding.icon.setImageResource(R.drawable.mw24_ic_circled_block_deny)
            }

            else -> {
                binding.rootLayout.isVisible = false
            }
        }
    }


}