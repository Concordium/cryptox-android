package com.concordium.wallet.ui.cis2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.FragmentTokensBinding
import com.concordium.wallet.ui.cis2.manage.ManageTokenListActivity

class TokensFragment : Fragment() {
    private var _binding: FragmentTokensBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TokensViewModel by lazy {
        ViewModelProvider(requireActivity())[TokensViewModel::class.java]
    }

    private lateinit var tokensAccountDetailsAdapter: TokensAccountDetailsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTokensBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
    }

    override fun onResume() {
        super.onResume()
        // Scroll to first element when account changed
        binding.tokensFound.smoothScrollToPosition(0)
    }

    private fun initViews() {
        binding.tokensFound.layoutManager = LinearLayoutManager(requireContext())
        tokensAccountDetailsAdapter = TokensAccountDetailsAdapter(
            context = requireContext(),
            showManageButton = true,
        )
        binding.tokensFound.adapter = tokensAccountDetailsAdapter

        tokensAccountDetailsAdapter.setTokenClickListener(object :
            TokensAccountDetailsAdapter.TokenClickListener {
            override fun onRowClick(token: Token) {
                viewModel.chooseToken.postValue(token)
            }
        })
    }

    private fun initObservers() {
        viewModel.waiting.observe(viewLifecycleOwner) { waiting ->
            if (!waiting) {
                binding.noItemsLayout.isVisible = viewModel.tokens.isEmpty()
            }

            viewModel.tokenData.account?.let { account ->
                tokensAccountDetailsAdapter.setManageButtonClickListener {
                    gotoManageTokensList(account)
                }
                binding.noItemsManageTokens.setOnClickListener {
                    gotoManageTokensList(account)
                }
            }
        }
        viewModel.tokenBalances.observe(viewLifecycleOwner) { ready ->
            showLoading(ready.not())
            if (ready) {
                binding.noItemsLayout.isVisible = viewModel.tokens.isEmpty()
                tokensAccountDetailsAdapter.setData(
                    viewModel.tokens.map { token ->
                        if (token.isCcd) {
                            token.copy(
                                isEarning = viewModel.tokenData.account?.isDelegating()!! ||
                                        viewModel.tokenData.account?.isBaking()!!
                            )
                        } else {
                            token
                        }
                    }
                )
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.loading.progressBar.isVisible = show
    }

    private fun gotoManageTokensList(account: Account) {
        val intent = Intent(requireActivity(), ManageTokenListActivity::class.java)
        intent.putExtra(ManageTokenListActivity.ACCOUNT, account)
        startActivity(intent)
    }
}
