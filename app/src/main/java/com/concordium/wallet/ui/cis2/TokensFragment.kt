package com.concordium.wallet.ui.cis2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.FragmentTokensBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsViewModel
import com.concordium.wallet.ui.cis2.manage.ManageTokenListActivity
import com.concordium.wallet.ui.cis2.manage.ManageTokensBottomSheet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TokensFragment : Fragment() {
    private var _binding: FragmentTokensBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TokensViewModel by lazy {
        ViewModelProvider(requireActivity())[TokensViewModel::class.java]
    }
    private val accountViewModel: AccountDetailsViewModel by viewModels()

    private var isFungible: Boolean? = null
    private lateinit var tokensAccountDetailsAdapter: TokensAccountDetailsAdapter
    private var manageTokensBottomSheet: ManageTokensBottomSheet? = null

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

            override fun onCheckBoxClick(token: Token) {
            }
        })
    }

    private fun initObservers() {
        accountViewModel.newAccount.collectWhenStarted(viewLifecycleOwner) { account ->
            tokensAccountDetailsAdapter.setData(viewModel.tokens)
            viewModel.loadTokens(account.address, isFungible)

            tokensAccountDetailsAdapter.setManageButtonClickListener {
                showFindTokensDialog(account)
            }
            binding.noItemsManageTokens.setOnClickListener {
                showFindTokensDialog(account)
            }
        }

        viewModel.waiting.observe(viewLifecycleOwner) {
            binding.noItemsLayout.isVisible = viewModel.tokens.isEmpty()
            tokensAccountDetailsAdapter.setData(viewModel.tokens)
            viewModel.loadTokensBalances()
        }
        viewModel.tokenDetails.observe(viewLifecycleOwner) {
            tokensAccountDetailsAdapter.notifyDataSetChanged()
        }
        viewModel.tokenBalances.observe(viewLifecycleOwner) {
            tokensAccountDetailsAdapter.notifyDataSetChanged()
        }
//        viewModel.updateWithSelectedTokensDone.observe(viewLifecycleOwner) {
//            viewLifecycleOwner.lifecycleScope.launch {
//                viewModel.loadTokens(accountViewModel.newAccount.first().address, isFungible)
//            }
//        }

        viewModel.updateWithSelectedTokensDone.observe(viewLifecycleOwner) { anyChanges ->
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.loadTokens(accountViewModel.newAccount.first().address, isFungible)
            }

            requireActivity().runOnUiThread {
                manageTokensBottomSheet = null
                if (anyChanges) {
                    Toast.makeText(
                        requireContext(),
                        R.string.cis_tokens_updated,
                        Toast.LENGTH_SHORT
                    ).show()
                } else
                    Toast.makeText(
                        requireContext(),
                        R.string.cis_tokens_not_updated,
                        Toast.LENGTH_SHORT
                    ).show()
            }
        }
    }

    private fun showFindTokensDialog(account: Account) {
        val intent = Intent(requireActivity(), ManageTokenListActivity::class.java)
        intent.putExtra(ManageTokenListActivity.ACCOUNT, account)
        startActivity(intent)
    }
}
