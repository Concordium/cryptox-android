package com.concordium.wallet.ui.account.accountdetails.transfers

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.model.TransactionOriginType
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.databinding.FragmentAccountDetailsTransfersBinding
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsViewModel
import com.concordium.wallet.ui.transaction.transactiondetails.TransactionDetailsActivity
import com.concordium.wallet.uicore.recyclerview.pinnedheader.PinnedHeaderItemDecoration

class AccountDetailsTransfersFragment : Fragment() {
    private var _binding: FragmentAccountDetailsTransfersBinding? = null
    private val binding get() = _binding!!

    private lateinit var accountDetailsViewModel: AccountDetailsViewModel
    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountDetailsTransfersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViewModel()
        initializeViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        accountDetailsViewModel.populateTransferList()
    }

    private fun initializeViewModel() {
        accountDetailsViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[AccountDetailsViewModel::class.java]

        accountDetailsViewModel.totalBalanceLiveData.observe(viewLifecycleOwner) {
            transactionAdapter.notifyDataSetChanged()
        }

        accountDetailsViewModel.transferListLiveData.observe(viewLifecycleOwner) { transferList ->
            transferList?.let {
                val filteredList = transferList.filterIndexed { index, currentItem ->
                    var result = true
                    if (currentItem.getItemType() == AdapterItem.ItemType.Header)
                        result = showHeader(transferList, index)
                    else if (currentItem.getItemType() == AdapterItem.ItemType.Item)
                        result = showItem(currentItem)
                    result
                }

                if (filteredList.isNotEmpty()) {
                    transactionAdapter.setData(filteredList)
                    transactionAdapter.removeFooter()
                    if (accountDetailsViewModel.hasMoreRemoteTransactionsToLoad) {
                        transactionAdapter.addFooter()
                    }
                    transactionAdapter.notifyDataSetChanged()
                }

                if (filteredList.isEmpty()) {
                    binding.noTransfersTextview.visibility = View.VISIBLE
                } else {
                    binding.noTransfersTextview.visibility = View.GONE
                }

                accountDetailsViewModel.allowScrollToLoadMore = true
            }
        }

        accountDetailsViewModel.showGTUDropLiveData.observe(viewLifecycleOwner) { waiting ->
            waiting?.let { show ->
                if (show) {
                    binding.gtuDropLayout.visibility = View.VISIBLE
                    binding.gtuDropButton.isEnabled = true
                } else {
                    binding.gtuDropLayout.visibility = View.GONE
                }
            }
        }
    }

    private fun showHeader(transferList: List<AdapterItem>, currentIndex: Int): Boolean {
        var show = false
        var index = currentIndex
        do {
            index++
            var nextItem: AdapterItem? = null
            if (transferList.size > index)
                nextItem = transferList[index]
            if (nextItem != null && nextItem.getItemType() == AdapterItem.ItemType.Item)
                show = showItem(nextItem)
        } while (nextItem != null && nextItem.getItemType() == AdapterItem.ItemType.Item && !show)
        return show
    }

    private fun showItem(currentItem: AdapterItem): Boolean {
        var result = true
        val item = currentItem as TransactionItem
        item.transaction?.let {
            val transaction: Transaction = it
            if (transaction.isRemoteTransaction()) {
                if (transaction.origin != null && transaction.details != null) {
                    if (transaction.origin.type != TransactionOriginType.Self && (transaction.details.type == TransactionType.ENCRYPTEDAMOUNTTRANSFER || transaction.details.type == TransactionType.ENCRYPTEDAMOUNTTRANSFERWITHMEMO)) {
                        result = false
                    }
                }
            }
        }
        return result
    }

    private fun initializeViews() {
        binding.noTransfersTextview.visibility = View.GONE
        binding.gtuDropLayout.visibility = View.GONE

        binding.gtuDropButton.setOnClickListener {
            binding.gtuDropButton.isEnabled = false
            accountDetailsViewModel.requestGTUDrop()
        }

        transactionAdapter = TransactionAdapter()

        val linearLayoutManager = LinearLayoutManager(context)
        binding.recyclerview.setHasFixedSize(true)
        binding.recyclerview.adapter = transactionAdapter
        binding.recyclerview.layoutManager = linearLayoutManager

        // Pinned Header
        val headerItemDecoration = PinnedHeaderItemDecoration(transactionAdapter)
        binding.recyclerview.addItemDecoration(headerItemDecoration)

        // Click
        transactionAdapter.setOnItemClickListener(object :
            TransactionAdapter.OnItemClickListener {
            override fun onItemClicked(item: Transaction) {
                val intent = Intent(activity, TransactionDetailsActivity::class.java)
                intent.putExtra(
                    TransactionDetailsActivity.EXTRA_ACCOUNT,
                    accountDetailsViewModel.account
                )
                intent.putExtra(TransactionDetailsActivity.EXTRA_TRANSACTION, item)
                startActivity(intent)
            }
        })

        binding.recyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (accountDetailsViewModel.allowScrollToLoadMore) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    if (visibleItemCount + firstVisibleItemPosition >= totalItemCount && firstVisibleItemPosition >= 0) {
                        accountDetailsViewModel.loadMoreRemoteTransactions()
                    }
                }
            }
        })
    }
}
