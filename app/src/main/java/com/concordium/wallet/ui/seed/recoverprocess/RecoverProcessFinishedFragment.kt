package com.concordium.wallet.ui.seed.recoverprocess

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.FragmentRecoverProcessFinishedBinding
import com.concordium.wallet.databinding.ItemIdentityWithAccountsAccountBinding
import com.concordium.wallet.databinding.ItemIdentityWithAccountsBinding
import com.concordium.wallet.ui.seed.recoverprocess.RecoverProcessViewModel.Companion.RECOVER_PROCESS_DATA

class RecoverProcessFinishedFragment : RecoverProcessBaseFragment() {
    private var _binding: FragmentRecoverProcessFinishedBinding? = null
    private val binding get() = _binding!!

    companion object {
        @JvmStatic
        fun newInstance(recoverProcessData: RecoverProcessData) =
            RecoverProcessFinishedFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(RECOVER_PROCESS_DATA, recoverProcessData)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecoverProcessFinishedBinding.inflate(inflater, container, false)
        initViews()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        if (recoverProcessData.noResponseFrom.size > 0) {
            binding.partialLayout.visibility = View.VISIBLE
            binding.providerNames.visibility = View.VISIBLE
            binding.providerNames.text = recoverProcessData.noResponseFrom.joinToString("\n") { it }
            binding.titleTextView.text = getString(R.string.seed_phrase_recover_process_partial)
        } else {
            binding.partialLayout.visibility = View.GONE
            binding.providerNames.visibility = View.GONE
            binding.titleTextView.text = getString(R.string.seed_phrase_recover_process_finished)
            if (recoverProcessData.identitiesWithAccounts.isNotEmpty())
                binding.subtitleTextView.text =
                    getString(R.string.seed_phrase_recover_process_recovered)
            else
                binding.subtitleTextView.text =
                    getString(R.string.seed_phrase_recover_process_nothing_new)
        }

        val layoutInflater = LayoutInflater.from(context)
        recoverProcessData.identitiesWithAccounts.forEach { identityWithAccounts ->
            val itemIdentityWithAccounts =
                ItemIdentityWithAccountsBinding.inflate(
                    layoutInflater,
                    binding.identitiesAccounts,
                    true
                )
            itemIdentityWithAccounts.identityName.text = identityWithAccounts.identity.name
            itemIdentityWithAccounts.identityAccountsCount.text = getString(
                R.string.seed_phrase_recover_process_finished_accounts_count,
                identityWithAccounts.accounts.size
            )

            identityWithAccounts.accounts.forEach { account ->
                val row = ItemIdentityWithAccountsAccountBinding.inflate(
                    layoutInflater,
                    itemIdentityWithAccounts.accounts,
                    true
                )
                row.accountNameTextView.text = account.getAccountName()
                row.balanceTextView.text =
                    getString(R.string.amount, CurrencyUtil.formatGTU(account.finalizedBalance))
            }

            // Empty row instead of accounts.
            if (identityWithAccounts.accounts.isEmpty()) {
                val row = ItemIdentityWithAccountsAccountBinding.inflate(
                    layoutInflater,
                    itemIdentityWithAccounts.accounts,
                    true
                )
                row.accountNameTextView.text =
                    getString(R.string.seed_phrase_recover_process_finished_no_accounts)
            }
        }
    }
}
