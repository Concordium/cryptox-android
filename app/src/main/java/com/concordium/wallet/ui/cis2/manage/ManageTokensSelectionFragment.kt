package com.concordium.wallet.ui.cis2.manage

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
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
                        from = _viewModel.tokens.last().uid,
                    )
                }
            }
        })
        initSearch()

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

    @SuppressLint("NotifyDataSetChanged")
    private fun initObservers() {
        _viewModel.lookForTokens.observe(viewLifecycleOwner) {
            binding.searchLayout.isVisible = _viewModel.tokens.size > 1
            selectionAdapter.dataSet = _viewModel.tokens.toTypedArray()
            selectionAdapter.notifyDataSetChanged()
            binding.searchLayout.setText("")
        }
        _viewModel.lookForExactToken.observe(viewLifecycleOwner) { status ->
            showWaiting(false)
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
        _viewModel.selectedTokensChanged.observe(viewLifecycleOwner) { changed ->
            binding.continueBtn.isEnabled = changed
        }
        _viewModel.nonSelected.observe(viewLifecycleOwner) { nonSelected ->
            if (nonSelected)
                binding.nonSelected.visibility = View.VISIBLE
            else
                binding.nonSelected.visibility = View.INVISIBLE
        }
    }

    private fun showWaiting(show: Boolean) {
        binding.includeProgress.progressBar.isVisible = show
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun resetSearch() {
        selectionAdapter.dataSet = _viewModel.tokens.toTypedArray()
        selectionAdapter.notifyDataSetChanged()
        _viewModel.dismissExactTokenLookup()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initSearch() {
        binding.searchLayout.apply {
            setInputType(InputType.TYPE_CLASS_TEXT)
            setSearchListener { onSearch() }
            setClearListener { setText("") }

            setTextChangeListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s.isNullOrBlank()) {
                        resetSearch()
                    }
                }
            })
            setOnSearchDoneListener { onSearch() }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun onSearch() {
        selectionAdapter.dataSet = emptyArray()
        selectionAdapter.notifyDataSetChanged()
        showWaiting(true)
        _viewModel.lookForExactToken(
            apparentTokenId = binding.searchLayout.getText().trim(),
            accountAddress = _viewModel.tokenData.account!!.address,
        )
    }

    private fun updateTokens() {
        _viewModel.updateWithSelectedTokens()
        _viewModel.updateWithSelectedTokensDone.observe(viewLifecycleOwner) { value ->
            val intent = Intent(requireContext(), ManageTokenListActivity::class.java).apply {
                putExtra(ManageTokenListActivity.ACCOUNT, _viewModel.tokenData.account)
                putExtra(ManageTokenListActivity.LIST_UPDATED, value)
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun gotoTokenDetails(token: Token) {
        val intent = Intent(requireContext(), AddTokenDetailsActivity::class.java).apply {
            putExtra(AddTokenDetailsActivity.TOKEN, token)
        }
        startActivity(intent)
    }
}
