package com.concordium.wallet.ui.tokens.tokens

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.Constants.PAGINATION_COUNT

class PaginationListener(private val layoutManager: GridLayoutManager, private val callback: IPaginationCallback) : RecyclerView.OnScrollListener() {

    interface IPaginationCallback {
        fun onLoadMore(page: Int, count: Int)
        fun getTokensCount(): Int
    }

    private var currentPage = 0
    private var previousTotalItemCount = 0
    private var loading = true

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
        val totalItemCount: Int = layoutManager.itemCount

        if (totalItemCount < previousTotalItemCount) {
            this.currentPage = 0
            this.previousTotalItemCount = totalItemCount
            if (totalItemCount == 0) {
                this.loading = true
            }
        }
        if (loading && totalItemCount > previousTotalItemCount) {
            loading = false
            previousTotalItemCount = totalItemCount
        }
        val visibleThreshold = 5
        val f1 = (lastVisibleItemPosition + visibleThreshold) > totalItemCount
        val f2 = totalItemCount >= PAGINATION_COUNT
        val f3 = !loading
        if (f3 && f1 && f2) {
            currentPage++
            callback.onLoadMore(callback.getTokensCount(), totalItemCount)
            loading = true
        }
    }
}