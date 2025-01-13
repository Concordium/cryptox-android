package com.concordium.wallet.ui.cis2.manage

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentManageTokensBottomSheetBinding
import com.concordium.wallet.ui.cis2.TokensViewModel

class ManageTokensBottomSheet : Fragment(
    R.layout.fragment_manage_tokens_bottom_sheet
) {

    private var _binding: FragmentManageTokensBottomSheetBinding? = null
    private val binding get() = _binding!!
    val viewModel: TokensViewModel
        get() = (requireActivity() as AddTokenActivity).viewModelTokens

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentManageTokensBottomSheetBinding.bind(view)
        initViews()
        initObservers()
    }

    override fun onResume() {
        super.onResume()
        binding.viewPager.currentItem = 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.viewPager.adapter = LookForNewTokensAdapter()
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.currentItem = 0
    }

    private fun initObservers() {
        viewModel.lookForTokens.observe(viewLifecycleOwner) {
            if (it != TokensViewModel.TOKENS_NOT_LOADED
                && viewModel.tokens.isNotEmpty()
                && binding.viewPager.currentItem == 0
            ) {
                binding.viewPager.currentItem++
            }
        }
        viewModel.stepPageBy.observe(viewLifecycleOwner) {
            val targetPosition = binding.viewPager.currentItem + it

            if (targetPosition == -1) {
                return@observe
            }

            if (targetPosition >= 0 && targetPosition < (binding.viewPager.adapter?.itemCount
                    ?: 0)
            ) {
                binding.viewPager.currentItem = targetPosition
            }

            if (binding.viewPager.currentItem == 0) {
                viewModel.tokens.clear()
            }
        }
    }

    private inner class LookForNewTokensAdapter : FragmentStateAdapter(this) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ManageTokensContractAddressFragment()
                1 -> ManageTokensSelectionFragment()
                2 -> ManageTokensTokenDetailsFragment()
                else -> throw IndexOutOfBoundsException("Unsupported fragment position $position")
            }
        }
    }
}
