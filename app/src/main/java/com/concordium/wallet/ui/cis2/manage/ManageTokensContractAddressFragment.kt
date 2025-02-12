package com.concordium.wallet.ui.cis2.manage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentManageTokensContractAddressBinding
import com.concordium.wallet.ui.cis2.TokensViewModel
import com.concordium.wallet.util.KeyboardUtil

class ManageTokensContractAddressFragment : Fragment() {
    private var _binding: FragmentManageTokensContractAddressBinding? = null
    private val binding get() = _binding!!
    private val _viewModel: TokensViewModel
        get() = (parentFragment as ManageTokensMainFragment).viewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageTokensContractAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.look.setOnClickListener {
            lookForTokens()
        }

//        binding.contractAddress.setOnFocusChangeListener { _, hasFocus ->
//            if (hasFocus)
//                showOrHideError(TokensViewModel.TOKENS_OK)
//        }
//
//        binding.contractAddress.setOnEditorActionListener { _, actionId, _ ->
//            if (actionId == EditorInfo.IME_ACTION_DONE) {
//                lookForTokens()
//                true
//            } else {
//                false
//            }
//        }
    }

    private fun initObservers() {
        _viewModel.lookForTokens.observe(viewLifecycleOwner) { result ->
            showWaiting(false)
            showOrHideError(result)
        }

        _viewModel.contractAddressLoading.observe(viewLifecycleOwner, ::showWaiting)
    }

    private fun lookForTokens() {
        val contractIndex = binding.searchLayout.getSearchText()
            .takeUnless(String::isNullOrBlank)
            ?: return

        showWaiting(true)
        KeyboardUtil.hideKeyboard(requireActivity())

        _viewModel.tokenData.contractIndex = contractIndex
        _viewModel.lookForTokens(_viewModel.tokenData.account!!.address)
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.look.isEnabled = false
            binding.includeProgress.progressLayout.isVisible = true
        } else {
            binding.look.isEnabled = true
            binding.includeProgress.progressLayout.isVisible = false
        }
    }

    private fun showOrHideError(result: Int) {
        when (result) {
            TokensViewModel.TOKENS_NOT_LOADED,
            TokensViewModel.TOKENS_OK -> {
                binding.error.visibility = View.GONE
            }

            else -> {
                binding.error.visibility = View.VISIBLE
                if (result == TokensViewModel.TOKENS_EMPTY)
                    binding.error.text = getString(R.string.cis_find_tokens_none)
                else
                    binding.error.text = getString(R.string.cis_find_tokens_error)
            }
        }
    }
}
