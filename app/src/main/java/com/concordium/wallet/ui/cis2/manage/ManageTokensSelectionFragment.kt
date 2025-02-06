package com.concordium.wallet.ui.cis2.manage

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.FragmentManageTokensSelectionBinding
import com.concordium.wallet.ui.cis2.TokensViewModel

class ManageTokensSelectionFragment : Fragment() {
    private var _binding: FragmentManageTokensSelectionBinding? = null
    private val binding get() = _binding!!
    private val _viewModel: TokensViewModel
        get() = (parentFragment as ManageTokensMainFragment).viewModel
    private lateinit var selectionAdapter: ManageTokensSelectionAdapter
    private var firstTime = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageTokensSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        initObservers()
    }

    override fun onResume() {
        super.onResume()

        if (!firstTime)
            selectionAdapter.notifyDataSetChanged()
        firstTime = false
        _viewModel.checkExistingTokens()
        binding.nonSelected.visibility = View.INVISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        selectionAdapter = ManageTokensSelectionAdapter(requireActivity(), arrayOf())
        selectionAdapter.dataSet = _viewModel.tokens.toTypedArray()

        binding.tokensFound.adapter = selectionAdapter
        binding.tokensFound.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                if (_viewModel.tokens.size > 0 &&
                    visibleItemCount + firstVisibleItemPosition >= totalItemCount &&
                    firstVisibleItemPosition >= 0 &&
                    totalItemCount > 3
                ) {
                    _viewModel.lookForTokens(
                        accountAddress = _viewModel.tokenData.account!!.address,
                        from = _viewModel.tokens[_viewModel.tokens.size - 1].uid,
                    )
                }
            }
        })

        binding.searchLayout.setSearchListener {
            selectionAdapter.dataSet = emptyArray()
            selectionAdapter.notifyDataSetChanged()

            _viewModel.lookForExactToken(
                apparentTokenId = binding.searchLayout.getSearchText().trim(),
                accountAddress = _viewModel.tokenData.account!!.address,
            )
        }

//        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//            override fun onQueryTextSubmit(query: String?): Boolean {
//                selectionAdapter.dataSet = emptyArray()
//                selectionAdapter.notifyDataSetChanged()
//
//                _viewModel.lookForExactToken(
//                    apparentTokenId = query?.trim() ?: "",
//                    accountAddress = _viewModel.tokenData.account!!.address,
//                )
//
//                return false
//            }
//
//            override fun onQueryTextChange(newText: String?): Boolean {
//                if (newText.isNullOrBlank()) {
//                    selectionAdapter.dataSet = _viewModel.tokens.toTypedArray()
//                    selectionAdapter.notifyDataSetChanged()
//
//                    _viewModel.dismissExactTokenLookup()
//                }
//                return true
//            }
//        })

//        binding.search.setOnQueryTextFocusChangeListener { _, hasFocus ->
//            if (!hasFocus) {
//                KeyboardUtil.hideKeyboard(requireActivity())
//            }
//        }

        selectionAdapter.setTokenClickListener(object :
            ManageTokensSelectionAdapter.TokenClickListener {
            override fun onRowClick(token: Token) {
                _viewModel.chooseTokenInfo.postValue(token)
                gotoTokenDetails(token)
            }

            override fun onCheckBoxClick(token: Token) {
                _viewModel.toggleNewToken(token)
            }
        })

        binding.continueBtn.setOnClickListener {
            updateTokens()
        }
    }

    private fun initObservers() {
        _viewModel.lookForTokens.observe(viewLifecycleOwner) {
            binding.searchLayout.isVisible = _viewModel.tokens.size > 1
            selectionAdapter.dataSet = _viewModel.tokens.toTypedArray()
            selectionAdapter.notifyDataSetChanged()
        }
        _viewModel.lookForExactToken.observe(viewLifecycleOwner) { status ->
            binding.noTokensFound.isVisible = status == TokensViewModel.TOKENS_EMPTY
            if (status == TokensViewModel.TOKENS_OK) {
                selectionAdapter.dataSet = arrayOf(checkNotNull(_viewModel.exactToken))
                selectionAdapter.notifyDataSetChanged()
            }
        }
        _viewModel.tokenDetails.observe(viewLifecycleOwner) {
            selectionAdapter.dataSet = _viewModel.tokens.toTypedArray()
            selectionAdapter.notifyDataSetChanged()
        }
        _viewModel.hasExistingTokens.observe(viewLifecycleOwner) { hasExistingTokens ->
            binding.continueBtn.isEnabled = true
        }
        _viewModel.nonSelected.observe(viewLifecycleOwner) { nonSelected ->
            if (nonSelected)
                binding.nonSelected.visibility = View.VISIBLE
            else
                binding.nonSelected.visibility = View.INVISIBLE
        }
    }

    private fun updateTokens() {
        _viewModel.updateWithSelectedTokens()
        val intent = Intent(requireContext(), ManageTokenListActivity::class.java).apply {
            putExtra(ManageTokenListActivity.ACCOUNT, _viewModel.tokenData.account)
            putExtra(ManageTokenListActivity.LIST_UPDATED, true)
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun gotoTokenDetails(token: Token) {
        val intent = Intent(requireContext(), AddTokenDetailsActivity::class.java).apply {
            putExtra(AddTokenDetailsActivity.TOKEN, token)
        }
        startActivity(intent)
    }
}
