package com.concordium.wallet.ui.walletconnect

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.concordium.sdk.crypto.wallet.web3Id.CredentialAttribute
import com.concordium.sdk.crypto.wallet.web3Id.Statement.RequestStatement

// TODO: move attributes, when we enable choosing account
class CredentialStatementAdapter(private val requests: List<RequestStatement>, private val attributes: Map<String, CredentialAttribute>):
    RecyclerView.Adapter<CredentialStatementAdapter.ViewHolder>() {

        class ViewHolder(val statements: DisplayStatements): RecyclerView.ViewHolder(statements)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = DisplayStatements(parent.context)
        view.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,RecyclerView.LayoutParams.MATCH_PARENT)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return requests.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        println(position)
        holder.statements.setStatement(requests.get(position), attributes)
    }
}
