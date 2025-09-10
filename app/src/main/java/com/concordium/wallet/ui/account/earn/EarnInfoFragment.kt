package com.concordium.wallet.ui.account.earn

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.AccountCooldown
import com.concordium.wallet.data.model.BakerDelegationData
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.FragmentEarnInfoBinding
import com.concordium.wallet.ui.bakerdelegation.baker.introflow.BakerRegistrationIntroFlow
import com.concordium.wallet.ui.bakerdelegation.common.CooldownView
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.bakerdelegation.delegation.DelegationRegisterAmountActivity
import com.concordium.wallet.ui.bakerdelegation.delegation.introflow.DelegationCreateIntroFlowActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.UnitConvertUtil
import org.koin.androidx.viewmodel.ext.android.viewModel

class EarnInfoFragment : Fragment() {

    private lateinit var binding: FragmentEarnInfoBinding
    private val viewModel: EarnInfoViewModel by viewModel()
    private val account: Account by lazy {
        requireArguments().getSerializable(EXTRA_ACCOUNT) as Account
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEarnInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        initObservers()
    }


    private fun initViews() {
        binding.btnBaker.setOnClickListener {
            gotoStartValidating()
        }
        binding.btnStartEarning.setOnClickListener {
            gotoStartEarning()
        }
        binding.btnReadMore.setOnClickListener {
            gotoReadMore()
        }
        addCooldowns(account.cooldowns)
    }

    private fun initObservers() {
        viewModel.error.observe(viewLifecycleOwner, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                (activity as BaseActivity).showError(value)
            }
        })
        viewModel.chainParameters.observe(viewLifecycleOwner) { chainParameters ->
            chainParameters?.delegatorCooldown?.let {
                val gracePeriod = UnitConvertUtil.secondsToDaysRoundedDown(it)
                binding.updateDescription.text =
                    resources.getQuantityString(
                        R.plurals.earn_delegation_update_description,
                        gracePeriod,
                        gracePeriod
                    )
            }
        }
    }

    private fun addCooldowns(cooldowns: Collection<AccountCooldown>) {
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

    private fun gotoStartEarning() {
        val intent = Intent(requireActivity(), DelegationRegisterAmountActivity::class.java)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            BakerDelegationData(account, type = ProxyRepository.REGISTER_DELEGATION)
        )
        (activity as BaseActivity).startActivityForResultAndHistoryCheck(intent)
    }

    private fun gotoReadMore() {
        val intent = Intent(requireActivity(), DelegationCreateIntroFlowActivity::class.java)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            BakerDelegationData(account, type = ProxyRepository.REGISTER_DELEGATION)
        )
        (activity as BaseActivity).startActivityForResultAndHistoryCheck(intent)
    }

    private fun gotoStartValidating() {
        val intent = Intent(requireActivity(), BakerRegistrationIntroFlow::class.java)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            BakerDelegationData(account, type = ProxyRepository.REGISTER_BAKER)
        )
        (activity as BaseActivity).startActivityForResultAndHistoryCheck(intent)
    }

    companion object {
        const val EXTRA_ACCOUNT = "extra_account"

        fun newInstance(bundle: Bundle) = EarnInfoFragment().apply {
            arguments = bundle
        }

        fun setBundle(account: Account) = Bundle().apply {
            putSerializable(EXTRA_ACCOUNT, account)
        }
    }
}