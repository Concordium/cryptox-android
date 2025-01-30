package com.concordium.wallet.ui.recipient.recipientlist

import android.animation.ObjectAnimator
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.databinding.ItemRecipientBinding

interface IListCallback {
    fun delete(item: Recipient)
    fun handleRowClick(item: Recipient)
}

class RecipientAdapter(private val callback: IListCallback) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var data: List<Recipient> = emptyList()
    private var allData: List<Recipient> = emptyList()
    private var currentFilter = ""

    private var swipedPos = -1
    private var lastSwipeLayout: SwipeLayout? = null

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)

        if (holder.adapterPosition != RecyclerView.NO_POSITION && holder is ItemViewHolder) {
            if (holder.adapterPosition == swipedPos && holder.view.swipe == lastSwipeLayout) {
                if (holder.view.swipe.offset == 0) {
                    lastSwipeLayout = null
                    swipedPos = -1
                }
            }
        }
    }

    inner class ItemViewHolder(
        val view: ItemRecipientBinding
    ) : RecyclerView.ViewHolder(view.root) {
        private val rootLayout: View = view.foregroundRoot
        private val nameTextView: TextView = view.recipientNameTextview
        private val addressTextView: TextView = view.recipientAddressTextview
        private val deleteBtn: ImageView = view.deleteContact

        fun bind(item: Recipient) {
            nameTextView.text = item.name
            addressTextView.text = item.address

            deleteBtn.setOnClickListener {
                callback.delete(item)
            }

            rootLayout.setOnClickListener {
                callback.handleRowClick(item)
            }
        }

        fun setSwipe(position: Int) {
            view.swipe.setOnSwipeListener(object : SwipeLayout.OnSwipeListener {
                override fun onBeginSwipe(swipeLayout: SwipeLayout?, moveToRight: Boolean) {
                    if (lastSwipeLayout != null && lastSwipeLayout != swipeLayout) {
                        lastSwipeLayout?.animateReset()
                    }
                    lastSwipeLayout = swipeLayout
                }

                override fun onSwipeClampReached(swipeLayout: SwipeLayout?, moveToRight: Boolean) {
                    swipedPos = adapterPosition
                }

                override fun onLeftStickyEdge(swipeLayout: SwipeLayout?, moveToRight: Boolean) {
                }

                override fun onRightStickyEdge(swipeLayout: SwipeLayout?, moveToRight: Boolean) {
                }
            })
            if (position == swipedPos) {
                view.rightDrag.post {
                    shift(-view.rightDrag.width, view.swipe)
                }
            } else {
                view.swipe.reset()
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
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder(
            ItemRecipientBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = data[position]
        with(holder as ItemViewHolder) {
            bind(item)
            setSwipe(position)
        }
    }

    fun setData(data: List<Recipient>) {
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

    fun get(position: Int): Recipient {
        return data[position]
    }

    fun filter(filterString: String?) {
        currentFilter = filterString ?: ""
        data = getFilteredList(allData, currentFilter)
        notifyDataSetChanged()
    }

    private fun getFilteredList(allData: List<Recipient>, filterString: String): List<Recipient> {
        return allData.filter { recipient ->
            recipient.name.lowercase().contains(filterString.lowercase()) ||
                    recipient.address.lowercase().contains(filterString.lowercase())
        }
    }
}