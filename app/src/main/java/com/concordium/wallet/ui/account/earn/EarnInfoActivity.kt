package com.concordium.wallet.ui.account.earn

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.AccountCooldown
import com.concordium.wallet.data.model.BakerDelegationData
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityEarnInfoBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.bakerdelegation.baker.introflow.BakerRegistrationIntroFlow
import com.concordium.wallet.ui.bakerdelegation.common.CooldownView
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.bakerdelegation.delegation.DelegationRegisterPoolActivity
import com.concordium.wallet.ui.bakerdelegation.delegation.introflow.DelegationCreateIntroFlowActivity
import com.concordium.wallet.ui.base.BaseActivity

class EarnInfoActivity : BaseActivity(R.layout.activity_earn_info, R.string.earn_title) {
    private lateinit var binding: ActivityEarnInfoBinding
    private lateinit var viewModel: EarnViewModel
    private lateinit var account: Account

    companion object {
        const val EXTRA_ACCOUNT_DATA = "EXTRA_ACCOUNT_DATA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEarnInfoBinding.bind(findViewById(R.id.toastLayoutTopError))
        hideActionBarBack(isVisible = true)
        account = intent.extras?.getSerializable(EXTRA_ACCOUNT_DATA) as Account
        initViews()
        initializeViewModel()
        initObservers()
        viewModel.loadChainParameters()
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
        viewModel.error.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[EarnViewModel::class.java]

        viewModel
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
        val intent = Intent(this, DelegationRegisterPoolActivity::class.java)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            BakerDelegationData(account, type = ProxyRepository.REGISTER_DELEGATION)
        )
        startActivityForResultAndHistoryCheck(intent)
        finishUntilClass(MainActivity::class.java.canonicalName)
    }

    private fun gotoReadMore() {
        val intent = Intent(this, DelegationCreateIntroFlowActivity::class.java)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            BakerDelegationData(account, type = ProxyRepository.REGISTER_DELEGATION)
        )
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun gotoStartValidating() {
        val intent = Intent(this, BakerRegistrationIntroFlow::class.java)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            BakerDelegationData(account, type = ProxyRepository.REGISTER_BAKER)
        )
        startActivityForResultAndHistoryCheck(intent)
    }
}
