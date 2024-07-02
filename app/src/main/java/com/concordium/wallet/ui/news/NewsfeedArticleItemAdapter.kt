package com.concordium.wallet.ui.news

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ListItemNewsfeedArticleBinding
import com.concordium.wallet.util.DateTimeUtil

class NewsfeedArticleItemAdapter(
    private val onItemClicked: (NewsfeedArticleListItem) -> Unit,
) : RecyclerView.Adapter<NewsfeedArticleItemAdapter.ViewHolder>() {
    private var data: List<NewsfeedArticleListItem> = listOf()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(items: List<NewsfeedArticleListItem>) {
        this.data = items
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int =
        data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_newsfeed_article, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]

        with(holder.binding) {
            titleTextView.text = item.title

            Glide.with(thumbnailImageView.context)
                .load(item.thumbnailUrl)
                .placeholder(R.color.ccx_neutral_tint_4)
                .centerCrop()
                .into(thumbnailImageView)

            if (item.description != null) {
                descriptionTextView.isVisible = true
                descriptionTextView.text = item.description
            } else {
                descriptionTextView.isVisible = false
            }

            dateTextView.text = DateTimeUtil.formatDate(item.date)

            root.setOnClickListener {
                onItemClicked(item)
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ListItemNewsfeedArticleBinding.bind(itemView)
    }
}
