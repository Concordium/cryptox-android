package com.concordium.wallet.ui.account.earn

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.databinding.FragmentEarnBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.common.delegates.EarnDelegate
import com.concordium.wallet.ui.common.delegates.EranDelegateImpl
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class EarnFragment : Fragment(), EarnDelegate by EranDelegateImpl() {

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

        viewModel.uiState.collectWhenStarted(viewLifecycleOwner) { state ->
            state.account?.let {
                launchEarn(
                    account = it,
                    hasPendingDelegationTransactions = state.hasPendingDelegationTransactions,
                    hasPendingBakingTransactions = state.hasPendingBakingTransactions,
                    launchFragment = ::replaceFragment
                )
            }
            binding.loading.progressBar.isVisible = state.loading
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(binding.earnContainer.id, fragment)
            .commit()
    }
}