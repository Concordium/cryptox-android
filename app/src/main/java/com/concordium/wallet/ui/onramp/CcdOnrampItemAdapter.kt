package com.concordium.wallet.ui.onramp

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ListItemCcdOnrampHeaderBinding
import com.concordium.wallet.databinding.ListItemCcdOnrampSectionBinding
import com.concordium.wallet.databinding.ListItemCcdOnrampSiteBinding
import com.concordium.wallet.uicore.handleUrlClicks

class CcdOnrampItemAdapter(
    private val onReadDisclaimerClicked: () -> Unit,
    private val onSiteClicked: (item: CcdOnrampListItem.Site) -> Unit,
    isDisclaimerAccepted: Boolean
) : RecyclerView.Adapter<CcdOnrampItemAdapter.ViewHolder>() {
    private var data: List<CcdOnrampListItem> = listOf()
    private var hasAcceptedDisclaimer: Boolean = isDisclaimerAccepted

    override fun getItemViewType(position: Int): Int = when (data[position]) {
        CcdOnrampListItem.Header ->
            R.layout.list_item_ccd_onramp_header

        is CcdOnrampListItem.Section ->
            R.layout.list_item_ccd_onramp_section

        is CcdOnrampListItem.Site ->
            R.layout.list_item_ccd_onramp_site

        CcdOnrampListItem.NoneAvailable ->
            R.layout.list_item_ccd_onramp_none_available

        CcdOnrampListItem.ExchangesNotice ->
            R.layout.list_item_ccd_onramp_exchanges_notice
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

            R.layout.list_item_ccd_onramp_exchanges_notice ->
                ViewHolder.ExchangesNotice(view)

            else ->
                error("Unknown view type $viewType")
        }
    }

    override fun getItemCount(): Int = data.size

    @SuppressLint("UseCompatTextViewDrawableApis")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder.Header -> {
                with(holder.binding.readDisclaimerButton) {
                    setOnClickListener { onReadDisclaimerClicked() }
                    if (hasAcceptedDisclaimer) {
                        setBackgroundResource(R.drawable.mw24_button_tertiary_background)
                        setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.mw24_button_tertiary_text_color
                            )
                        )
                        setCompoundDrawablesRelativeWithIntrinsicBounds(
                            0, 0, R.drawable.mw24_ic_circled_check_done, 0
                        )
                        compoundDrawableTintList = ContextCompat.getColorStateList(
                            context,
                            R.color.mw24_button_tertiary_text_color
                        )
                    }
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

                    root.setOnClickListener {
                        onSiteClicked(item)
                    }
                }
            }

            is ViewHolder.NoneAvailable,
            is ViewHolder.ExchangesNotice,
            -> {
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(items: List<CcdOnrampListItem>) {
        this.data = items
        notifyDataSetChanged()
    }

    fun updateHeaderDisclaimerButton(isDisclaimerAccepted: Boolean) {
        this.hasAcceptedDisclaimer = isDisclaimerAccepted
        val headerIndex = data.indexOfFirst { it is CcdOnrampListItem.Header }
        if (headerIndex != -1) {
            notifyItemChanged(headerIndex, DISCLAIMER_PAYLOAD)
        }
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

        class ExchangesNotice(itemView: View) : ViewHolder(itemView) {
            init {
                (itemView as TextView).handleUrlClicks { url ->
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    ContextCompat.startActivity(itemView.context, browserIntent, null)
                }
            }
        }
    }

    companion object {
        private const val DISCLAIMER_PAYLOAD = "disclaimer_accepted"
    }
}
