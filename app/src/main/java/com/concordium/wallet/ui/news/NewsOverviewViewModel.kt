package com.concordium.wallet.ui.news

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.backend.news.NewsfeedRepository
import com.concordium.wallet.util.Log
import kotlinx.coroutines.launch

class NewsOverviewViewModel(application: Application) : AndroidViewModel(application) {
    private val newsfeedRepository: NewsfeedRepository by lazy {
        NewsfeedRepository(App.appCore.getNewsfeedRssBackend())
    }

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean> = _waitingLiveData
    private val _listItemsLiveData = MutableLiveData<List<NewsfeedArticleListItem>>()
    val listItemsLiveData: LiveData<List<NewsfeedArticleListItem>> = _listItemsLiveData
    private val _isLoadingFailedVisibleLiveData = MutableLiveData(false)
    val isLoadingFailedVisibleLiveData: LiveData<Boolean> = _isLoadingFailedVisibleLiveData

    init {
        App.appCore.tracker.homeNewsScreen()
        loadNews()
    }

    private fun loadNews() = viewModelScope.launch {
        _waitingLiveData.postValue(true)
        _isLoadingFailedVisibleLiveData.postValue(false)

        try {
            val articles = newsfeedRepository.getArticles(
                limit = ARTICLES_LIMIT
            )

            _listItemsLiveData.postValue(articles.map(::NewsfeedArticleListItem))
            _isLoadingFailedVisibleLiveData.postValue(false)
        } catch (e: Exception) {
            Log.e("Failed loading news", e)

            _isLoadingFailedVisibleLiveData.postValue(true)
        }

        _waitingLiveData.postValue(false)
    }


    fun onReloadClicked() {
        loadNews()
    }

    private companion object {
        private const val ARTICLES_LIMIT = 10
    }
}
