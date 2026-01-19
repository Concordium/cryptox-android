package com.concordium.wallet.ui.plt

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.data.model.ProtocolLevelToken
import com.concordium.wallet.databinding.ViewPltAllowDenyListBinding

class PLTAllowDenyListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val binding: ViewPltAllowDenyListBinding

    init {
        LayoutInflater.from(context).inflate(R.layout.view_plt_allow_deny_list, this, true)
        binding = ViewPltAllowDenyListBinding.bind(this)
    }

    fun setToken(
        token: ProtocolLevelToken,
        onTokenLabelClick: () -> Unit,
        onTokenStatusClick: () -> Unit,
        onTokenPausedClick: () -> Unit,
    ) {
        val pltListStatus = getPLTPLTListStatus(token)

        when (pltListStatus) {
            PLTListStatus.PAUSED -> {
                binding.pltListStatusLayout.background = AppCompatResources.getDrawable(
                    context,
                    R.drawable.plt_paused_background
                )
                binding.listStatusTitle.text =
                    context.getString(R.string.account_details_token_paused)
                binding.listStatusTitle.setTextColor(context.getColor(R.color.mw24_content_warning_primary))
                binding.listStatusIcon.setImageResource(R.drawable.mw24_ic_circled_block_warning)
                binding.listStatusQuestionIcon.imageTintList =
                    AppCompatResources.getColorStateList(
                        context,
                        R.color.mw24_content_warning_primary
                    )
            }

            PLTListStatus.ON_ALLOW_LIST -> {
                binding.pltListStatusLayout.background = AppCompatResources.getDrawable(
                    context,
                    R.drawable.label_success_background
                )
                binding.listStatusTitle.text =
                    context.getString(R.string.account_details_account_on_allow_list)
                binding.listStatusTitle.setTextColor(context.getColor(R.color.mw24_content_success_primary))
                binding.listStatusIcon.setImageResource(R.drawable.mw24_ic_circled_check_done)
                binding.listStatusQuestionIcon.imageTintList =
                    AppCompatResources.getColorStateList(
                        context,
                        R.color.mw24_content_success_primary
                    )
            }

            PLTListStatus.NOT_ON_ALLOW_LIST -> {
                binding.pltListStatusLayout.background = AppCompatResources.getDrawable(
                    context,
                    R.drawable.label_warning_background
                )
                binding.listStatusTitle.text =
                    context.getString(R.string.account_details_account_not_on_allow_list)
                binding.listStatusTitle.setTextColor(context.getColor(R.color.mw24_content_warning_primary))
                binding.listStatusIcon.setImageResource(R.drawable.mw24_ic_circled_block_warning)
                binding.listStatusQuestionIcon.imageTintList =
                    AppCompatResources.getColorStateList(
                        context,
                        R.color.mw24_content_warning_primary
                    )
            }

            PLTListStatus.ON_DENY_LIST -> {
                binding.pltListStatusLayout.background = AppCompatResources.getDrawable(
                    context,
                    R.drawable.label_error_background
                )
                binding.listStatusTitle.text =
                    context.getString(R.string.account_details_account_on_deny_list)
                binding.listStatusTitle.setTextColor(context.getColor(R.color.mw24_content_error_primary))
                binding.listStatusIcon.setImageResource(R.drawable.mw24_ic_circled_block_deny)
                binding.listStatusQuestionIcon.imageTintList =
                    AppCompatResources.getColorStateList(
                        context,
                        R.color.mw24_content_error_primary
                    )
            }

            else -> binding.pltListStatusLayout.isVisible = false
        }

        binding.pltLabelLayout.setOnClickListener { onTokenLabelClick() }
        binding.pltListStatusLayout.setOnClickListener {
            when (pltListStatus) {
                PLTListStatus.PAUSED -> onTokenPausedClick()
                else -> onTokenStatusClick()
            }
        }
    }

    private fun getPLTPLTListStatus(token: ProtocolLevelToken): PLTListStatus = when {
        token.isPaused -> PLTListStatus.PAUSED
        (token.isInDenyList != null && token.isInDenyList) -> PLTListStatus.ON_DENY_LIST
        (token.isInAllowList != null && token.isInAllowList) -> PLTListStatus.ON_ALLOW_LIST
        token.isInAllowList != null -> PLTListStatus.NOT_ON_ALLOW_LIST
        else -> PLTListStatus.UNKNOWN
    }
}