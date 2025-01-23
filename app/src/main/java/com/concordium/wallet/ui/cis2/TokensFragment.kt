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
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.FragmentTokensBinding
import com.concordium.wallet.ui.cis2.manage.ManageTokenListActivity
import com.concordium.wallet.uicore.toast.showGradientToast

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

    private fun initViews() {
        binding.tokensFound.layoutManager = LinearLayoutManager(activity)
        tokensAccountDetailsAdapter = TokensAccountDetailsAdapter(
            context = requireContext(),
            showManageButton = true,
        )
        tokensAccountDetailsAdapter.also { binding.tokensFound.adapter = it }

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
                println("tokensfragment newAccount: ${viewModel.tokenData.account?.address}")
                binding.noItemsLayout.isVisible = viewModel.tokens.isEmpty()
                tokensAccountDetailsAdapter.setData(viewModel.tokens)
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
        viewModel.tokenDetails.observe(viewLifecycleOwner) {
            tokensAccountDetailsAdapter.notifyDataSetChanged()
        }
        viewModel.tokenBalances.observe(viewLifecycleOwner) {
            tokensAccountDetailsAdapter.notifyDataSetChanged()
        }

        viewModel.updateWithSelectedTokensDone.observe(viewLifecycleOwner) { anyChanges ->
            if (anyChanges) {
                requireContext().showGradientToast(
                    R.drawable.mw24_ic_address_copy_check,
                    getString(R.string.cis_tokens_updated)
                )
            } else {
                requireContext().showGradientToast(
                    R.drawable.mw24_ic_address_copy_check,
                    getString(R.string.cis_tokens_not_updated)
                )
            }
        }
    }

    private fun gotoManageTokensList(account: Account) {
        val intent = Intent(requireActivity(), ManageTokenListActivity::class.java)
        intent.putExtra(ManageTokenListActivity.ACCOUNT, account)
        startActivity(intent)
    }
}
