package com.concordium.wallet.data.backend.news

import com.concordium.wallet.data.model.NewsfeedEntry

class NewsfeedRepository(
    private val backend: NewsfeedRssBackend,
) {
    suspend fun getArticles(limit: Int? = null): List<NewsfeedEntry.Article> {
        return backend.getArticlesItems()
            .let { items ->
                if (limit != null)
                    items.subList(0, limit.coerceAtMost(items.size))
                else
                    items
            }
            .map(NewsfeedEntry::Article)
    }
}
