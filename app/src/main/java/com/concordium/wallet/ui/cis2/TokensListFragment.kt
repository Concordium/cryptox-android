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
import com.concordium.wallet.data.model.NewToken
import com.concordium.wallet.databinding.FragmentTokensListBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsViewModel
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.manage.ManageTokenListActivity
import kotlinx.coroutines.FlowPreview
import java.io.Serializable

class TokensListFragment : Fragment() {
    private var _binding: FragmentTokensListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AccountDetailsViewModel by lazy {
        ViewModelProvider(requireActivity())[AccountDetailsViewModel::class.java]
    }
    private val tokensListViewModel: TokensListViewModel by lazy {
        ViewModelProvider(requireActivity())[TokensListViewModel::class.java]
    }

    private lateinit var tokensAccountDetailsAdapter: TokensAccountDetailsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTokensListBinding.inflate(inflater, container, false)
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
        binding.tokensList.smoothScrollToPosition(0)
    }

    private fun initViews() {
        binding.tokensList.layoutManager = LinearLayoutManager(requireContext())
        tokensAccountDetailsAdapter = TokensAccountDetailsAdapter(
            context = requireContext(),
            showManageButton = true,
        )
        binding.tokensList.adapter = tokensAccountDetailsAdapter

        tokensAccountDetailsAdapter.setTokenClickListener(object :
            TokensAccountDetailsAdapter.TokenClickListener {
            override fun onRowClick(token: NewToken) {
                goToTokenDetails(token)
            }
        })
        tokensAccountDetailsAdapter.setManageButtonClickListener {
            gotoManageTokensList()
        }
    }

    @OptIn(FlowPreview::class)
    private fun initObservers() {
        viewModel
            .accountUpdatedFlow
            .collectWhenStarted(viewLifecycleOwner) { updatedAccount ->
                tokensListViewModel.loadTokens(updatedAccount)
            }

        tokensListViewModel.uiState.collectWhenStarted(viewLifecycleOwner) { uiState ->
            showLoading(uiState.isLoading)
            tokensAccountDetailsAdapter.setData(uiState.tokens)
            uiState.error?.let {
                it.contentOrNullIfUsed?.let { res ->
                    (requireActivity() as BaseActivity).showError(res)
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.loading.progressBar.isVisible = show
    }

    private fun gotoManageTokensList() {
        val intent = Intent(requireActivity(), ManageTokenListActivity::class.java)
        intent.putExtra(ManageTokenListActivity.ACCOUNT, viewModel.account)
        startActivity(intent)
    }

    private fun goToTokenDetails(token: NewToken) {
        val intent = Intent(requireActivity(), TokenDetailsActivity::class.java).apply {
            putExtra(TokenDetailsActivity.ACCOUNT, viewModel.account)
            putExtra(TokenDetailsActivity.TOKEN, token as Serializable)
            putExtra(
                TokenDetailsActivity.PENDING_DELEGATION,
                viewModel.hasPendingDelegationTransactions
            )
            putExtra(
                TokenDetailsActivity.PENDING_VALIDATION,
                viewModel.hasPendingBakingTransactions
            )
        }
        startActivity(intent)
    }
}
