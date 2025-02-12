package com.concordium.wallet.ui.cis2.manage

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentManageTokensMainBinding
import com.concordium.wallet.ui.cis2.TokensViewModel
import com.concordium.wallet.ui.cis2.TokensViewModel.Companion.TOKENS_EMPTY
import com.concordium.wallet.ui.cis2.TokensViewModel.Companion.TOKENS_NOT_LOADED
import com.concordium.wallet.ui.cis2.TokensViewModel.Companion.TOKENS_OK
import com.concordium.wallet.ui.cis2.TokensViewModel.Companion.TOKENS_SELECTED
import com.concordium.wallet.util.KeyboardUtil

class ManageTokensMainFragment : Fragment(
    R.layout.fragment_manage_tokens_main
) {

    private var _binding: FragmentManageTokensMainBinding? = null
    private val binding get() = _binding!!
    val viewModel: TokensViewModel
        get() = (requireActivity() as AddTokenActivity).viewModelTokens

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentManageTokensMainBinding.bind(view)
        initViews()
        initObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.searchLayout.apply {
            setSearchListener { lookForTokens() }
            setOnSearchDoneListener { lookForTokens() }
            setClearListener {
                binding.searchLayout.setSearchText("")
                viewModel.tokens.clear()
                viewModel.lookForTokens.postValue(TOKENS_NOT_LOADED)
            }
        }
    }

    private fun initObservers() {
        viewModel.lookForTokens.observe(viewLifecycleOwner) { state ->
            updateViews(state)
        }

        viewModel.contractAddressLoading.observe(viewLifecycleOwner) {
            showWaiting(it)
        }

//            if (tokens != TokensViewModel.TOKENS_NOT_LOADED
//                && viewModel.tokens.isNotEmpty()
//                && binding.viewPager.currentItem == 0
//            ) {
//                binding.viewPager.currentItem++
//            }

//        viewModel.stepPageBy.observe(viewLifecycleOwner) {
//            val targetPosition = binding.viewPager.currentItem + it
//
//            if (targetPosition == -1) {
//                return@observe
//            }
//
//            if (targetPosition >= 0 && targetPosition < (binding.viewPager.adapter?.itemCount
//                    ?: 0)
//            ) {
//                binding.viewPager.currentItem = targetPosition
//            }
//
//            if (binding.viewPager.currentItem == 0) {
//                viewModel.tokens.clear()
//            }
//        }
    }

    private fun lookForTokens() {
        val contractIndex = binding.searchLayout.getSearchText()
            .takeUnless(String::isNullOrBlank)
            ?: return
        KeyboardUtil.hideKeyboard(requireActivity())

        viewModel.tokenData.contractIndex = contractIndex
        viewModel.lookForTokens(viewModel.tokenData.account!!.address)
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressBar.isVisible = waiting
    }

    private fun updateViews(result: Int) {
        when (result) {
            TOKENS_NOT_LOADED -> {
                binding.error.visibility = View.GONE
                binding.selectionFragmentContainer.visibility = View.GONE
            }

            TOKENS_SELECTED,
            TOKENS_OK -> {
                binding.error.visibility = View.GONE
                binding.selectionFragmentContainer.visibility = View.VISIBLE
            }

            else -> {
                binding.error.visibility = View.VISIBLE
                binding.selectionFragmentContainer.visibility = View.GONE
                if (result == TOKENS_EMPTY)
                    binding.error.text = getString(R.string.cis_find_tokens_none)
                else
                    binding.error.text = getString(R.string.cis_find_tokens_error)
            }
        }
    }
}
