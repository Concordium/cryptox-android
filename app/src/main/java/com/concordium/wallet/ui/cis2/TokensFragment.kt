package com.concordium.wallet.ui.cis2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.FragmentTokensBinding
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsViewModel
import com.concordium.wallet.ui.cis2.manage.ManageTokenListActivity
import com.concordium.wallet.ui.cis2.manage.ManageTokensBottomSheet

class TokensFragment : Fragment() {
    private var _binding: FragmentTokensBinding? = null
    private val binding get() = _binding!!
    val viewModel: TokensViewModel by lazy {
        ViewModelProvider(requireActivity())[TokensViewModel::class.java]
    }
    private val accountViewModel: AccountDetailsViewModel by lazy {
        ViewModelProvider(requireActivity())[AccountDetailsViewModel::class.java]
    }

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
        viewModel.loadTokens(accountViewModel.account.address, isFungible)
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

        tokensAccountDetailsAdapter.setManageButtonClickListener {
            showFindTokensDialog()
        }
        binding.noItemsManageTokens.setOnClickListener {
            showFindTokensDialog()
        }
    }

    private fun initObservers() {
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
        viewModel.updateWithSelectedTokensDone.observe(viewLifecycleOwner) {
            viewModel.loadTokens(accountViewModel.account.address, isFungible)
        }

        viewModel.updateWithSelectedTokensDone.observe(viewLifecycleOwner) { anyChanges ->
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

    private fun showFindTokensDialog() {
        val intent = Intent(requireActivity(), ManageTokenListActivity::class.java)
        intent.putExtra(ManageTokenListActivity.ACCOUNT, accountViewModel.account)
        startActivity(intent)
    }
}
