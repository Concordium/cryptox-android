package com.concordium.wallet.ui.onramp

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ListItemCcdOnrampHeaderBinding
import com.concordium.wallet.databinding.ListItemCcdOnrampSectionBinding
import com.concordium.wallet.databinding.ListItemCcdOnrampSiteBinding

class CcdOnrampItemAdapter(
    private val onReadDisclaimerClicked: () -> Unit,
    private val onSiteClicked: (item: CcdOnrampListItem.Site) -> Unit,
) : RecyclerView.Adapter<CcdOnrampItemAdapter.ViewHolder>() {
    private var data: List<CcdOnrampListItem> = listOf()

    override fun getItemViewType(position: Int): Int = when (data[position]) {
        CcdOnrampListItem.Header ->
            R.layout.list_item_ccd_onramp_header

        is CcdOnrampListItem.Section ->
            R.layout.list_item_ccd_onramp_section

        is CcdOnrampListItem.Site ->
            R.layout.list_item_ccd_onramp_site

        CcdOnrampListItem.NoneAvailable ->
            R.layout.list_item_ccd_onramp_none_available

        CcdOnrampListItem.Disclaimer ->
            R.layout.list_item_ccd_onramp_disclaimer
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(viewType, parent, false)

        return when (viewType) {
            R.layout.list_item_ccd_onramp_header ->
                ViewHolder.Header(view)

            R.layout.list_item_ccd_onramp_section ->
                ViewHolder.Section(view)

            R.layout.list_item_ccd_onramp_site ->
                ViewHolder.Site(view)

            R.layout.list_item_ccd_onramp_none_available ->
                ViewHolder.NoneAvailable(view)

            R.layout.list_item_ccd_onramp_disclaimer ->
                ViewHolder.Disclaimer(view)

            else ->
                error("Unknown view type $viewType")
        }
    }

    override fun getItemCount(): Int =
        data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder.Header -> {
                holder.binding.readDisclaimerButton.setOnClickListener {
                    onReadDisclaimerClicked()
                }
            }

            is ViewHolder.Section -> {
                val item = data[position] as CcdOnrampListItem.Section
                holder.binding.root.setText(item.nameRes)
            }

            is ViewHolder.Site -> {
                val item = data[position] as CcdOnrampListItem.Site

                with(holder.binding) {
                    nameTextView.text = item.name

                    Glide.with(logoImageView.context)
                        .load(item.logoUrl)
                        .placeholder(R.drawable.circle_bg)
                        .circleCrop()
                        .into(logoImageView)

                    creditCardImageView.isVisible = item.isCreditCardVisible
                    divider.isVisible = item.isDividerVisible

                    root.setOnClickListener {
                        onSiteClicked(item)
                    }
                }
            }

            is ViewHolder.NoneAvailable -> {}

            is ViewHolder.Disclaimer -> {}
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(items: List<CcdOnrampListItem>) {
        this.data = items
        notifyDataSetChanged()
    }

    sealed class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        class Header(itemView: View) : ViewHolder(itemView) {
            val binding = ListItemCcdOnrampHeaderBinding.bind(itemView)
        }

        class Section(itemView: View) : ViewHolder(itemView) {
            val binding = ListItemCcdOnrampSectionBinding.bind(itemView)
        }

        class Site(itemView: View) : ViewHolder(itemView) {
            val binding = ListItemCcdOnrampSiteBinding.bind(itemView)
        }

        class NoneAvailable(itemView: View) : ViewHolder(itemView)

        class Disclaimer(itemView: View) : ViewHolder(itemView)
    }
}
