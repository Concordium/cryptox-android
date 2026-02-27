package com.concordium.wallet.ui.multinetwork

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ListItemNetworkListNetworkBinding
import com.concordium.wallet.uicore.button.IconButton

class NetworkListItemAdapter(
    private val onNetworkItemClicked: (item: NetworkListItem.Network) -> Unit,
    private val onAddClicked: () -> Unit,
) : RecyclerView.Adapter<NetworkListItemAdapter.ViewHolder>() {

    private var data: List<NetworkListItem> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newData: List<NetworkListItem>) {
        data = newData
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int =
        data.size

    override fun getItemViewType(position: Int): Int =
        when (data[position]) {
            NetworkListItem.AddButton ->
                R.layout.list_item_network_list_add

            is NetworkListItem.Network ->
                R.layout.list_item_network_list_network
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)

        return when (viewType) {
            R.layout.list_item_network_list_network ->
                ViewHolder.Network(view)

            R.layout.list_item_network_list_add ->
                ViewHolder.AddButton(view)

            else ->
                error("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = when (holder) {
        is ViewHolder.AddButton ->
            holder.button.setOnClickListener {
                onAddClicked()
            }

        is ViewHolder.Network -> {
            val item = data[position] as NetworkListItem.Network

            with(holder.binding) {
                root.setOnClickListener {
                    onNetworkItemClicked(item)
                }
                root.isEnabled = !(item.isEditing && !item.isEditable)
                root.alpha =
                    if (root.isEnabled)
                        1f
                    else
                        0.5f
                root.isActivated = item.isConnected
//                root.background = ContextCompat.getDrawable(
//                    root.context,
//                    if (item.isConnected)
//                        R.drawable.mw24_container_primary_background_active
//                    else
//                        R.drawable.mw24_container_primary_background
//                )

                nameTextView.text = item.name

                actionTextView.setText(
                    when {
                        item.isEditing && item.isEditable ->
                            R.string.network_list_edit

                        !item.isConnected ->
                            R.string.network_list_connect

                        else ->
                            R.string.network_list_connected
                    }
                )
                actionTextView.alpha =
                    if (item.isEditing || !item.isConnected)
                        0.6f
                    else
                        1f
                actionTextView.setTextColor(
                    ContextCompat.getColor(
                        root.context,
                        if (!item.isEditing && item.isConnected)
                            R.color.mw24_content_success_primary
                        else
                            R.color.mw24_content_primary
                    )
                )
            }
        }
    }

    sealed class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        class AddButton(itemView: View) : ViewHolder(itemView) {
            val button: IconButton = (itemView as ViewGroup).findViewById(R.id.add_button)
        }

        class Network(itemView: View) : ViewHolder(itemView) {
            val binding = ListItemNetworkListNetworkBinding.bind(itemView)
        }
    }
}
