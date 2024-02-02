package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.FragmentSelectTokenBottomSheetBinding
import com.concordium.wallet.ui.cis2.SendTokenViewModel.Companion.SEND_TOKEN_DATA
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SelectTokenBottomSheet : BottomSheetDialogFragment(
    R.layout.fragment_select_token_bottom_sheet
) {
    override fun getTheme() =
        R.style.AppBottomSheetDialogTheme

    private var _binding: FragmentSelectTokenBottomSheetBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokensAccountDetailsAdapter: TokensAccountDetailsAdapter
    private lateinit var _viewModel: SendTokenViewModel
    private lateinit var _viewModelTokens: TokensViewModel

    companion object {
        @JvmStatic
        fun newInstance(viewModel: SendTokenViewModel, viewModelTokens: TokensViewModel) =
            SelectTokenBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(SEND_TOKEN_DATA, viewModel.sendTokenData)
                }
                _viewModel = viewModel
                _viewModelTokens = viewModelTokens
                _viewModelTokens.tokenData.account = _viewModel.sendTokenData.account
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSelectTokenBottomSheetBinding.bind(view)
        initViews()
        initObservers()
        _viewModel.loadTokens(_viewModel.sendTokenData.account?.address ?: "")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.tokensFound.layoutManager = LinearLayoutManager(activity)
        tokensAccountDetailsAdapter = TokensAccountDetailsAdapter(
            requireActivity(),
            showManageButton = false,
            dataSet = arrayOf(),
        )
        tokensAccountDetailsAdapter.also { binding.tokensFound.adapter = it }

        tokensAccountDetailsAdapter.setTokenClickListener(object :
            TokensAccountDetailsAdapter.TokenClickListener {
            override fun onRowClick(token: Token) {
                _viewModel.chooseToken.postValue(token)
            }

            override fun onCheckBoxClick(token: Token) {
            }
        })
    }

    private fun initObservers() {
        _viewModel.waiting.observe(viewLifecycleOwner) { waiting ->
            showWaiting(waiting)
        }
        _viewModel.tokens.observe(viewLifecycleOwner) { tokens ->
            tokensAccountDetailsAdapter.dataSet = tokens.toTypedArray()
            tokensAccountDetailsAdapter.notifyDataSetChanged()
            _viewModelTokens.tokens = tokens as MutableList<Token>
            _viewModelTokens.loadTokensBalances()
        }
        _viewModelTokens.tokenBalances.observe(viewLifecycleOwner) {
            tokensAccountDetailsAdapter.dataSet = _viewModelTokens.tokens.toTypedArray()
            tokensAccountDetailsAdapter.notifyDataSetChanged()
        }
    }

    private fun showWaiting(waiting: Boolean) {
        binding.tokensFound.visibility = if (waiting) View.GONE else View.VISIBLE
    }
}
