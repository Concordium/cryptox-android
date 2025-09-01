package com.concordium.wallet.ui.bakerdelegation.common

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.model.AccountCooldown
import com.concordium.wallet.data.model.BakerDelegationData
import com.concordium.wallet.data.model.PendingChange
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.DelegationBakerStatusContentItemBinding
import com.concordium.wallet.databinding.DelegationbakerStatusBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.DateTimeUtil.formatTo
import com.concordium.wallet.util.DateTimeUtil.toDate

abstract class StatusActivity(
    titleId: Int = R.string.app_name,
) : BaseActivity(R.layout.delegationbaker_status, titleId) {
    protected lateinit var binding: DelegationbakerStatusBinding
    protected lateinit var viewModel: DelegationBakerViewModel

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DelegationbakerStatusBinding.bind(findViewById(R.id.root_layout))
        hideActionBarBack(isVisible = true)

        initializeViewModel()
        viewModel.initialize(
            intent.extras?.getSerializable(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA)
                    as BakerDelegationData
        )
        initView()
    }

    protected open fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[DelegationBakerViewModel::class.java]
    }

    fun setContentTitle(res: Int) {
        binding.statusTextView.text = getString(res)
    }

    fun addContent(titleRes: Int, text: String, visibleDivider: Boolean = true) {
        addContent(title = getString(titleRes), text = text, visibleDivider = visibleDivider)
    }

    fun addContent(
        title: String,
        text: String,
        titleTextColor: Int? = null,
        visibleDivider: Boolean = true,
    ) {
        binding.statusEmptyTextView.visibility = View.GONE
        binding.statusListContainer.visibility = View.VISIBLE

        val delegationBakerStatusBinding =
            DelegationBakerStatusContentItemBinding.inflate(layoutInflater)

        if (title.isNotEmpty()) {
            delegationBakerStatusBinding.statusItemTitle.text = title
            titleTextColor?.let {
                delegationBakerStatusBinding.statusItemTitle.setTextColor(getColor(it))
            }
            delegationBakerStatusBinding.divider.visibility = View.GONE
        } else
            delegationBakerStatusBinding.statusItemTitle.visibility = View.GONE

        if (text.isNotEmpty()) {
            delegationBakerStatusBinding.statusItemContent.text = text
            if (visibleDivider)
                delegationBakerStatusBinding.divider.visibility = View.VISIBLE
            else
                delegationBakerStatusBinding.divider.visibility = View.GONE
        } else {
            delegationBakerStatusBinding.statusItemContent.visibility = View.GONE
        }

        binding.statusListContainer.addView(delegationBakerStatusBinding.root)
    }

    fun setExplanation(explanation: String) {
        binding.statusExplanationTextView.isVisible = true
        binding.statusExplanationTextView.text = explanation
    }

    fun setEmptyState(text: String) {
        binding.statusEmptyTextView.text = text
        binding.statusEmptyTextView.visibility = View.VISIBLE
        binding.statusListContainer.visibility = View.GONE
    }

    fun clearState() {
        binding.statusEmptyTextView.text = ""
        binding.statusListContainer.removeAllViews()
        binding.statusButtonTop.isEnabled = true
        binding.statusButtonBottom.isEnabled = true
        binding.statusExplanationTextView.isVisible = false
    }

    protected fun addPendingChange(
        pendingChange: PendingChange,
    ) {
        val estimatedChangeTime = pendingChange.estimatedChangeTime
            ?: return
        val prefix = estimatedChangeTime.toDate()?.formatTo("yyyy-MM-dd")
        val postfix = estimatedChangeTime.toDate()?.formatTo("HH:mm")
        val dateStr = getString(R.string.delegation_status_effective_time, prefix, postfix)
        addContent(
            getString(R.string.delegation_status_content_take_effect_on) + "\n" + dateStr,
            ""
        )
        if (pendingChange.change == "RemoveStake") {
            binding.statusButtonTop.isEnabled = false
            addContent(
                getString(R.string.delegation_status_content_delegation_will_be_stopped),
                ""
            )
        } else if (pendingChange.change == "ReduceStake") {
            pendingChange.newStake?.let { newStake ->
                addContent(
                    getString(R.string.delegation_status_new_amount),
                    CurrencyUtil.formatGTU(newStake)
                )
            }
        }
    }

    protected fun addWaitingForTransaction(contentTitleStringId: Int, emptyStateStringId: Int) {
        binding.statusButtonTop.isEnabled = false
        binding.statusButtonBottom.isEnabled = false
        binding.statusTextView.setTextColor(getColor(R.color.cryptox_white_main))
        setContentTitle(contentTitleStringId)
        setEmptyState(getString(emptyStateStringId))
    }

    protected fun addCooldowns(cooldowns: Collection<AccountCooldown>) {
        binding.cooldownListContainer.removeAllViews()
        cooldowns.forEach { cooldown ->
            val cooldownView =
                layoutInflater.inflate(
                    R.layout.item_cooldown,
                    binding.cooldownListContainer,
                    false
                ) as CooldownView
            cooldownView.setCooldown(cooldown)
            binding.cooldownListContainer.addView(cooldownView)
        }
    }

    abstract fun initView()

    protected open fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.includeProgress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.includeProgress.progressLayout.visibility = View.GONE
        }
    }
}
