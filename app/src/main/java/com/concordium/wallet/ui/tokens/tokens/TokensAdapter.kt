package com.concordium.wallet.ui.tokens.tokens

import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.ui.tokens.provider.Token

class TokensAdapter(private val callback: TokenItemView.ITokenItemView) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {

    private var data: MutableList<Token> = mutableListOf()
    private var dataOrig: MutableList<Token> = mutableListOf()
    private var walletName: String? = null

    class ItemViewHolder(val view: TokenItemView) : RecyclerView.ViewHolder(view) {

        fun bind(item: Token) {
            view.setToken(item)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder(
            TokenItemView(parent.context, walletName, null, callback)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = data[position]
        (holder as ItemViewHolder).bind(item)
        holder.setIsRecyclable(false)
    }

    fun setData(data: List<Token>, walletName: String) {
        this.data.addAll(data)
        this.dataOrig.addAll(data)
        this.walletName = walletName
        callback.showFoundCount(this.data.size)
        notifyDataSetChanged()
    }

    fun setNextData(nextData: List<Token>, walletName: String) {
        this.data.addAll(nextData)
        this.walletName = walletName
        callback.showFoundCount(this.data.size)
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(term: CharSequence?): FilterResults {
                val filterResults = FilterResults()

                val filteredList = mutableListOf<Token>()
                if (term.isNullOrEmpty()) {
                    filteredList.addAll(dataOrig)
                } else {
                    val f = dataOrig.filter { it.nftName.lowercase().startsWith(term.toString().lowercase()) }
                    filteredList.addAll(f)
                }

                filterResults.values = filteredList
                filterResults.count = filteredList.size

                return filterResults
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                val res = p1?.values as? List<Token>
                data.clear()
                if (!res.isNullOrEmpty()) {
                    data.addAll(res)
                }
                callback.showFoundCount(data.size)
                notifyDataSetChanged()
            }
        }
    }
}
