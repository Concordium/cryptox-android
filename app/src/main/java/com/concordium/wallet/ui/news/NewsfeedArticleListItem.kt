package com.concordium.wallet.ui.news

import com.concordium.wallet.data.model.NewsfeedEntry
import java.util.Date

class NewsfeedArticleListItem(
    val title: String,
    val thumbnailUrl: String,
    val description: String?,
    val date: Date,
    val source: NewsfeedEntry.Article?,
) {
    constructor(source: NewsfeedEntry.Article) : this(
        title = source.title,
        thumbnailUrl = source.thumbnailUrl,
        description = source.description,
        date = source.date,
        source = source,
    )
}
