package com.concordium.wallet.ui.news

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.App
import com.concordium.wallet.data.model.NewsfeedEntry
import com.concordium.wallet.databinding.FragmentNewsOverviewBinding
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.base.BaseFragment

class NewsOverviewFragment : BaseFragment() {

    private val viewModel: NewsOverviewViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        ).get()
    }
    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        ).get()
    }
    private lateinit var binding: FragmentNewsOverviewBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewsOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModel()
        initializeViews()

        (requireActivity() as BaseActivity).hideLeftPlus(isVisible = false)
    }

    override fun onResume() {
        super.onResume()
        App.appCore.tracker.discoverScreen()
    }

    private fun initializeViewModel() {
        viewModel.waitingLiveData.observe(viewLifecycleOwner) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }

        viewModel.isLoadingFailedVisibleLiveData.observe(viewLifecycleOwner) { isLoadingFailedVisible ->
            binding.reloadButton.isVisible = isLoadingFailedVisible
            binding.loadingFailedTextView.isVisible = isLoadingFailedVisible
        }
    }

    private fun initializeViews() {
        val adapter = NewsfeedArticleItemAdapter(
            onItemClicked = { item: NewsfeedArticleListItem ->
                item.source?.also(::onArticleClicked)
            },
        )
        binding.recyclerview.adapter = adapter
        viewModel.listItemsLiveData.observe(viewLifecycleOwner, adapter::setData)

        binding.reloadButton.setOnClickListener {
            viewModel.onReloadClicked()
        }
    }

    private fun onArticleClicked(article: NewsfeedEntry.Article) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
        startActivity(Intent.createChooser(browserIntent, article.title))
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }
}
