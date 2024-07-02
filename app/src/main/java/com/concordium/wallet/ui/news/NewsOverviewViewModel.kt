package com.concordium.wallet.ui.news

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.backend.news.NewsfeedRepository
import kotlinx.coroutines.launch

class NewsOverviewViewModel(application: Application) : AndroidViewModel(application) {
    private val newsfeedRepository: NewsfeedRepository by lazy {
        NewsfeedRepository(App.appCore.getNewsfeedRssBackend())
    }

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean> = _waitingLiveData
    private val _listItemsLiveData = MutableLiveData<List<NewsfeedArticleListItem>>()
    val listItemsLiveData: LiveData<List<NewsfeedArticleListItem>> = _listItemsLiveData

    init {
        loadNews()
    }

    private fun loadNews() = viewModelScope.launch {
        val articles = newsfeedRepository.getArticles(
            limit = ARTICLES_LIMIT
        )

        _listItemsLiveData.postValue(articles.map(::NewsfeedArticleListItem))
        _waitingLiveData.postValue(false)
    }

    private companion object {
        private const val ARTICLES_LIMIT = 10
    }
}
