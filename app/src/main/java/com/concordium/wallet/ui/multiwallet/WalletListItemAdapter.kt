package com.concordium.wallet.ui.multiwallet

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.databinding.ListItemWalletListAddWalletBinding
import com.concordium.wallet.databinding.ListItemWalletListWalletBinding

class WalletListItemAdapter(
    val onWalletClicked: (item: WalletListItem.Wallet) -> Unit,
    val onAddClicked: (walletType: AppWallet.Type) -> Unit,
) : RecyclerView.Adapter<WalletListItemAdapter.ViewHolder>() {

    private var data: List<WalletListItem> = emptyList()

    override fun getItemCount(): Int =
        data.size

    override fun getItemViewType(position: Int): Int = when (data[position]) {
        is WalletListItem.AddButton ->
            R.layout.list_item_wallet_list_add_wallet

        is WalletListItem.Wallet ->
            R.layout.list_item_wallet_list_wallet
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)

        return when (viewType) {
            R.layout.list_item_wallet_list_add_wallet ->
                ViewHolder.AddButton(view)

            R.layout.list_item_wallet_list_wallet ->
                ViewHolder.Wallet(view)

            else ->
                error("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = when (holder) {
        is ViewHolder.AddButton -> {
            val item = data[position] as WalletListItem.AddButton

            with(holder.binding) {
                root.setOnClickListener{
                    onAddClicked(item.walletType)
                }

                titleTextView.setText(
                    when (item.walletType) {
                        AppWallet.Type.FILE ->
                            R.string.wallet_list_add_file_wallet

                        AppWallet.Type.SEED ->
                            R.string.wallet_list_add_seed_wallet
                    }
                )
            }
        }

        is ViewHolder.Wallet -> {
            val item = data[position] as WalletListItem.Wallet

            with(holder.binding) {
                nameTextView.setText(
                    when (item.type) {
                        AppWallet.Type.FILE ->
                            R.string.wallet_list_file_wallet

                        AppWallet.Type.SEED ->
                            R.string.wallet_list_seed_wallet
                    }
                )

                statusTextView.setText(
                    if (item.isSelected) {
                        R.string.wallet_list_status_active_selected
                    } else {
                        R.string.wallet_list_status_active
                    }
                )

                indicatorRadioButton.isChecked = item.isSelected

                root.background =
                    if (item.isSelected) {
                        ContextCompat.getDrawable(
                            root.context,
                            R.drawable.wallet_list_item_selected_background
                        )
                    } else {
                        ContextCompat.getDrawable(
                            root.context,
                            R.drawable.wallet_list_item_background
                        )
                    }
                root.setOnClickListener {
                    onWalletClicked(item)
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(items: List<WalletListItem>) {
        this.data = items
        notifyDataSetChanged()
    }

    sealed class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        class AddButton(itemView: View) : ViewHolder(itemView) {
            val binding = ListItemWalletListAddWalletBinding.bind(itemView)
        }

        class Wallet(itemView: View) : ViewHolder(itemView) {
            val binding = ListItemWalletListWalletBinding.bind(itemView)
        }
    }
}
