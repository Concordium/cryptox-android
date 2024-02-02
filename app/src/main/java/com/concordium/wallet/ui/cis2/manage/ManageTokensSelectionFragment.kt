package com.concordium.wallet.ui.cis2.manage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.FragmentManageTokensSelectionBinding
import com.concordium.wallet.ui.cis2.TokensViewModel

class ManageTokensSelectionFragment : Fragment() {
    private var _binding: FragmentManageTokensSelectionBinding? = null
    private val binding get() = _binding!!
    private val _viewModel: TokensViewModel
        get() = (parentFragment as ManageTokensBottomSheet).viewModel
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
        _viewModel.hasExistingTokens()
        binding.nonSelected.visibility = View.INVISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.tokensFound.layoutManager = LinearLayoutManager(activity)
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
                if (_viewModel.tokens.size > 0 && visibleItemCount + firstVisibleItemPosition >= totalItemCount && firstVisibleItemPosition >= 0 && totalItemCount > 3) {
                    _viewModel.lookForTokens(
                        _viewModel.tokenData.account!!.address,
                        from = _viewModel.tokens[_viewModel.tokens.size - 1].id
                    )
                }
            }
        })

        selectionAdapter.setTokenClickListener(object :
            ManageTokensSelectionAdapter.TokenClickListener {
            override fun onRowClick(token: Token) {
                _viewModel.chooseTokenInfo.postValue(token)
                _viewModel.stepPage(1)
            }

            override fun onCheckBoxClick(token: Token) {
                _viewModel.toggleNewToken(token)
            }
        })

        binding.back.setOnClickListener {
            _viewModel.stepPage(-1)
        }

        binding.updateWithTokens.setOnClickListener {
            _viewModel.updateWithSelectedTokens()
        }
    }

    private fun initObservers() {
        _viewModel.lookForTokens.observe(viewLifecycleOwner) {
            selectionAdapter.dataSet = _viewModel.tokens.toTypedArray()
            _viewModel.searchedTokens.clear()
            selectionAdapter.notifyDataSetChanged()
        }
        _viewModel.tokenDetails.observe(viewLifecycleOwner) {
            selectionAdapter.dataSet = _viewModel.tokens.toTypedArray()
            selectionAdapter.notifyDataSetChanged()
        }
        _viewModel.hasExistingAccountContract.observe(viewLifecycleOwner) { hasExistingAccountContract ->
            if (hasExistingAccountContract)
                binding.updateWithTokens.text = getString(R.string.cis_update_tokens)
            else
                binding.updateWithTokens.text = getString(R.string.cis_add_tokens)
            binding.updateWithTokens.isEnabled = true
        }
        _viewModel.nonSelected.observe(viewLifecycleOwner) { nonSelected ->
            if (nonSelected)
                binding.nonSelected.visibility = View.VISIBLE
            else
                binding.nonSelected.visibility = View.INVISIBLE
        }
    }
}
