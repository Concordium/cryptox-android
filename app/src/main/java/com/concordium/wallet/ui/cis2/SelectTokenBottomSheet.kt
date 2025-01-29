package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.ActivitySelectTokenBinding
import com.concordium.wallet.ui.cis2.SendTokenViewModel.Companion.SEND_TOKEN_DATA
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SelectTokenBottomSheet : BottomSheetDialogFragment(
    R.layout.activity_select_token
) {
    override fun getTheme() =
        R.style.AppBottomSheetDialogTheme

    private var _binding: ActivitySelectTokenBinding? = null
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
        _binding = ActivitySelectTokenBinding.bind(view)
        initViews()
        initObservers()
        _viewModel.loadTokens(_viewModel.sendTokenData.account?.address ?: "")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.tokensList.layoutManager = LinearLayoutManager(activity)
        tokensAccountDetailsAdapter = TokensAccountDetailsAdapter(
            context = requireActivity(),
            showManageButton = false
        )
        tokensAccountDetailsAdapter.also { binding.tokensList.adapter = it }

        tokensAccountDetailsAdapter.setTokenClickListener(object :
            TokensAccountDetailsAdapter.TokenClickListener {
            override fun onRowClick(token: Token) {
                _viewModel.chooseToken.postValue(token)
            }
        })
    }

    private fun initObservers() {
        _viewModel.waiting.observe(viewLifecycleOwner) { waiting ->
            showWaiting(waiting)
        }
        _viewModel.tokens.observe(viewLifecycleOwner) { tokens ->
            tokensAccountDetailsAdapter.setData(tokens)
            _viewModelTokens.tokens = tokens as MutableList<Token>
            _viewModelTokens.loadTokensBalances()
        }
        _viewModelTokens.tokenBalances.observe(viewLifecycleOwner) {
            tokensAccountDetailsAdapter.setData(_viewModelTokens.tokens)
        }
    }

    private fun showWaiting(waiting: Boolean) {
        binding.tokensList.visibility = if (waiting) View.GONE else View.VISIBLE
    }
}
