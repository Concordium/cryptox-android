package com.concordium.wallet.ui.walletconnect

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.concordium.sdk.crypto.wallet.web3Id.CredentialAttribute
import com.concordium.sdk.crypto.wallet.web3Id.Statement.RequestStatement
import com.concordium.wallet.databinding.IdentityProofContainerBinding
import com.concordium.wallet.extension.collect

// TODO: move attributes, when we enable choosing account
class CredentialStatementAdapter(private val requests: List<RequestStatement>, private val attributes: Map<String, CredentialAttribute>, val viewModel: WalletConnectViewModel, val lifecycleOwner: LifecycleOwner):
    RecyclerView.Adapter<CredentialStatementAdapter.ViewHolder>() {

        class ViewHolder(val containerBinding: IdentityProofContainerBinding): RecyclerView.ViewHolder(containerBinding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = IdentityProofContainerBinding.inflate(LayoutInflater.from(parent.context))
        binding.root.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,RecyclerView.LayoutParams.MATCH_PARENT)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return requests.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.containerBinding.statements.setStatement(requests.get(position), attributes)
    }
}
