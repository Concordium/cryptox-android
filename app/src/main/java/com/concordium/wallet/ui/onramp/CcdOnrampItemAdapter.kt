package com.concordium.wallet.ui.onramp

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ListItemCcdOnrampHeaderBinding
import com.concordium.wallet.databinding.ListItemCcdOnrampSectionBinding
import com.concordium.wallet.databinding.ListItemCcdOnrampSiteBinding
import com.concordium.wallet.databinding.ListItemCcdOnrampWidgetBinding

class CcdOnrampItemAdapter(
    private val onReadDisclaimerClicked: () -> Unit,
    private val onSiteClicked: (item: CcdOnrampListItem.Site) -> Unit,
    private val chromeClient: SwipeluxWebChromeClient,
    private val context: Context
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

        CcdOnrampListItem.SwipeluxWidget ->
            R.layout.list_item_ccd_onramp_widget
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

            R.layout.list_item_ccd_onramp_widget ->
                ViewHolder.SwipeluxWidget(view)

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

            is ViewHolder.SwipeluxWidget -> {
                holder.binding.root.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        // Request the keyboard if focus is gained
                        val inputMethodManager =
                            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
                    }
                }

                val webSettings = holder.binding.swipeluxWidgetLayout.settings

                with(webSettings) {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    javaScriptCanOpenWindowsAutomatically = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                holder.binding.swipeluxWidgetLayout.webChromeClient = chromeClient
                holder.binding.swipeluxWidgetLayout.isNestedScrollingEnabled = true
                holder.binding.swipeluxWidgetLayout.loadUrl("${SwipeluxWebChromeClient.BASE_URL}/?specificSettings=${chromeClient.specificSettings}")
            }
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

        class SwipeluxWidget(itemView: View) : ViewHolder(itemView) {
            val binding = ListItemCcdOnrampWidgetBinding.bind(itemView)
        }

        class NoneAvailable(itemView: View) : ViewHolder(itemView)

        class Disclaimer(itemView: View) : ViewHolder(itemView)
    }
}
