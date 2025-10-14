package com.concordium.wallet.ui.bakerdelegation.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.concordium.wallet.R
import com.concordium.wallet.data.model.AccountCooldown
import com.concordium.wallet.data.model.PendingChange
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.DelegationBakerStatusContentItemBinding
import com.concordium.wallet.databinding.DelegationbakerStatusFragmentBinding
import com.concordium.wallet.util.DateTimeUtil.formatTo
import com.concordium.wallet.util.DateTimeUtil.toDate
import org.koin.androidx.viewmodel.ext.android.viewModel


abstract class EarnStatusFragment : Fragment() {

    protected val viewModel: DelegationBakerViewModel by viewModel()
    protected lateinit var binding: DelegationbakerStatusFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DelegationbakerStatusFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        initView()
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
                delegationBakerStatusBinding.statusItemTitle.setTextColor(
                    requireActivity().getColor(
                        it
                    )
                )
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

    abstract fun initViewModel()

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
        binding.statusTextView.setTextColor(requireActivity().getColor(R.color.cryptox_white_main))
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