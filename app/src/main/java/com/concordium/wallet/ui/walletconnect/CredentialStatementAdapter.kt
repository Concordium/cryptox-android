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
        println(position)
        holder.containerBinding.statements.setStatement(requests.get(position), attributes)

        holder.containerBinding.declineButton.setOnClickListener {
            viewModel.rejectSessionRequest()
        }

        if (position == itemCount - 1) {

            holder.containerBinding.approveButton.setOnClickListener {
                viewModel.approveSessionRequest()
            }
            holder.containerBinding.approveButton.isEnabled = true
        } else {
            holder.containerBinding.approveButton.isEnabled = false
        }

        viewModel.isSessionRequestApproveButtonEnabledFlow.collect(
            lifecycleOwner = lifecycleOwner,
            action = holder.containerBinding.approveButton::setEnabled
        )

    }
}
