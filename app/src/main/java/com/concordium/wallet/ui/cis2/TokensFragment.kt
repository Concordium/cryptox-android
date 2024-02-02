package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.FragmentTokensBinding
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.cis2.manage.ManageTokensBottomSheet

class TokensFragment : Fragment() {
    private var _binding: FragmentTokensBinding? = null
    private val binding get() = _binding!!
    val viewModel: TokensViewModel
        get() = (requireActivity() as AccountDetailsActivity).viewModelTokens
    private val accountAddress: String
        get() = (requireActivity() as AccountDetailsActivity).viewModelAccountDetails.account.address
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
        viewModel.loadTokens(accountAddress, isFungible)
    }

    private fun initViews() {
        binding.tokensFound.layoutManager = LinearLayoutManager(activity)
        tokensAccountDetailsAdapter = TokensAccountDetailsAdapter(
            requireContext(),
            showManageButton = true,
            dataSet = arrayOf(),
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
    }

    private fun initObservers() {
        viewModel.waiting.observe(viewLifecycleOwner) {
            binding.noItems.isVisible = viewModel.tokens.isEmpty()
            tokensAccountDetailsAdapter.dataSet = viewModel.tokens.toTypedArray()
            tokensAccountDetailsAdapter.notifyDataSetChanged()
            viewModel.loadTokensBalances()
        }
        viewModel.tokenDetails.observe(viewLifecycleOwner) {
            tokensAccountDetailsAdapter.notifyDataSetChanged()
        }
        viewModel.tokenBalances.observe(viewLifecycleOwner) {
            tokensAccountDetailsAdapter.notifyDataSetChanged()
        }
        viewModel.updateWithSelectedTokensDone.observe(viewLifecycleOwner) {
            viewModel.loadTokens(accountAddress, isFungible)
        }

        viewModel.updateWithSelectedTokensDone.observe(viewLifecycleOwner) { anyChanges ->
            requireActivity().runOnUiThread {
                manageTokensBottomSheet?.dismiss()
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
        manageTokensBottomSheet = ManageTokensBottomSheet()
        manageTokensBottomSheet?.show(childFragmentManager, "")
    }
}
