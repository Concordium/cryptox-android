package com.concordium.wallet.ui.recipient.recipientlist

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ItemRecipientBinding
import com.concordium.wallet.databinding.ItemRecipientCategoryBinding

interface IListCallback {
    fun delete(item: RecipientListItem.RecipientItem)
    fun handleRowClick(item: RecipientListItem.RecipientItem)
}

@SuppressLint("NotifyDataSetChanged")
class RecipientAdapter(
    private val callback: IListCallback, private val isSelectMode: Boolean
) : RecyclerView.Adapter<RecipientAdapter.ItemViewHolder>() {

    private var data: List<RecipientListItem> = emptyList()
    private var allData: List<RecipientListItem> = emptyList()
    private var currentFilter = ""

    private var swipedPos = -1
    private var lastSwipeLayout: SwipeLayout? = null

    override fun onViewDetachedFromWindow(holder: ItemViewHolder) {
        super.onViewDetachedFromWindow(holder)

        when (holder) {
            is ItemViewHolder.Recipient -> {
                if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    if (holder.bindingAdapterPosition == swipedPos && holder.binding.swipe == lastSwipeLayout) {
                        if (holder.binding.swipe.offset == 0) {
                            lastSwipeLayout = null
                            swipedPos = -1
                        }
                    }
                }
            }

            else -> {}
        }
    }


    override fun getItemViewType(position: Int): Int = when (data[position]) {
        is RecipientListItem.Category -> R.layout.item_recipient_category
        is RecipientListItem.RecipientItem -> R.layout.item_recipient
    }


    sealed class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        class Category(itemView: View) : ItemViewHolder(itemView) {
            val binding = ItemRecipientCategoryBinding.bind(itemView)
        }

        class Recipient(itemView: View) : ItemViewHolder(itemView) {
            val binding = ItemRecipientBinding.bind(itemView)
        }
    }

    override fun getItemCount(): Int = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)

        return when (viewType) {
            R.layout.item_recipient -> ItemViewHolder.Recipient(view)
            R.layout.item_recipient_category -> ItemViewHolder.Category(view)
            else -> error("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(
        holder: ItemViewHolder, @SuppressLint("RecyclerView") position: Int
    ) {
        when (holder) {
            is ItemViewHolder.Recipient -> {
                val item = data[position] as RecipientListItem.RecipientItem

                holder.binding.recipientNameTextview.text = item.name
                holder.binding.recipientAddressTextview.text = item.address
                holder.binding.arrow.isVisible = !isSelectMode

                holder.binding.deleteContact.setOnClickListener {
                    callback.delete(item)
                }

                holder.binding.foregroundRoot.setOnClickListener {
                    callback.handleRowClick(item)
                }

                holder.binding.swipe.setOnSwipeListener(object : SwipeLayout.OnSwipeListener {
                    override fun onBeginSwipe(swipeLayout: SwipeLayout?, moveToRight: Boolean) {
                        if (lastSwipeLayout != null && lastSwipeLayout != swipeLayout) {
                            lastSwipeLayout?.animateReset()
                        }
                        lastSwipeLayout = swipeLayout
                    }

                    override fun onSwipeClampReached(
                        swipeLayout: SwipeLayout?, moveToRight: Boolean
                    ) {
                        swipedPos = position
                    }

                    override fun onLeftStickyEdge(swipeLayout: SwipeLayout?, moveToRight: Boolean) {
                    }

                    override fun onRightStickyEdge(
                        swipeLayout: SwipeLayout?, moveToRight: Boolean
                    ) {
                    }
                })
                if (position == swipedPos) {
                    holder.binding.rightDrag.post {
                        shift(-holder.binding.rightDrag.width, holder.binding.swipe)
                    }
                } else {
                    holder.binding.swipe.reset()
                }
            }

            is ItemViewHolder.Category -> {
                val item = data[position] as RecipientListItem.Category
                holder.binding.root.setText(item.nameRes)
            }
        }
    }

    private fun shift(offset: Int, target: SwipeLayout) {
        val animator = ObjectAnimator()
        animator.target = target
        animator.setPropertyName("offset")
        animator.interpolator = LinearInterpolator() // default: AccelerateInterpolator
        animator.setIntValues(offset)
        animator.duration = 10L
        animator.start()
    }

    fun setData(data: List<RecipientListItem>) {
        if (allData.isEmpty()) {
            this.data = data
            this.allData = data
            notifyDataSetChanged()
            return
        }

        // Update internal lists with the new data
        this.allData = data
        if (!TextUtils.isEmpty(currentFilter)) {
            this.data = getFilteredList(data, currentFilter)
        } else {
            this.data = data
        }

        // Perform change animations
        // diffResult.dispatchUpdatesTo(this)
        notifyDataSetChanged()
    }

    fun filter(filterString: String?) {
        currentFilter = filterString ?: ""
        data = getFilteredList(allData, currentFilter)
        notifyDataSetChanged()
    }

    private fun getFilteredList(
        allData: List<RecipientListItem>, filterString: String
    ): List<RecipientListItem> {
        return allData.filter { it is RecipientListItem.RecipientItem }.filter { recipient ->
            recipient as RecipientListItem.RecipientItem
            recipient.name.lowercase()
                .contains(filterString.lowercase()) || recipient.address.lowercase()
                .contains(filterString.lowercase())
        }
    }
}
