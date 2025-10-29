package com.concordium.wallet.ui.account.earn

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.FragmentEarnBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.bakerdelegation.baker.BakerStatusFragment
import com.concordium.wallet.ui.bakerdelegation.delegation.DelegationStatusFragment
import com.concordium.wallet.ui.base.BaseActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class EarnFragment : Fragment() {

    private lateinit var binding: FragmentEarnBinding

    private val viewModel: EarnViewModel by viewModel {
        parametersOf(ViewModelProvider(requireActivity())[MainViewModel::class.java])
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEarnBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initToolbar()
        viewModel.uiState.collectWhenStarted(viewLifecycleOwner) { state ->
            binding.loading.progressBar.isVisible = state.loading

            if (!state.loading && state.account != null) {
                launchEarn(
                    account = state.account,
                    hasPendingDelegationTransactions = state.hasPendingDelegationTransactions,
                    hasPendingBakingTransactions = state.hasPendingBakingTransactions,
                    launchFragment = ::replaceFragment
                )
            }
        }
    }

    private fun initToolbar() {
        val baseActivity = (activity as BaseActivity)
        baseActivity.hideQrScan(isVisible = false)
        baseActivity.hideSettings(isVisible = false)
    }

    private fun replaceFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(binding.earnContainer.id, fragment)
            .commit()
    }

    private fun launchEarn(
        account: Account,
        hasPendingDelegationTransactions: Boolean,
        hasPendingBakingTransactions: Boolean,
        launchFragment: (Fragment) -> Unit
    ) {
        when {
            (account.delegation != null || hasPendingDelegationTransactions) -> {
                launchFragment(
                    DelegationStatusFragment.newInstance(
                        DelegationStatusFragment.setBundle(
                            account,
                            hasPendingDelegationTransactions
                        )
                    )
                )
            }

            (account.baker != null || hasPendingBakingTransactions) -> {
                launchFragment(
                    BakerStatusFragment.newInstance(
                        BakerStatusFragment.setBundle(
                            account,
                            hasPendingBakingTransactions
                        )
                    )
                )
            }

            else -> launchFragment(
                EarnInfoFragment.newInstance(EarnInfoFragment.setBundle(account))
            )
        }
    }
}