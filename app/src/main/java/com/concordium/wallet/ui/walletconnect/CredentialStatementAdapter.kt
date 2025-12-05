package com.concordium.wallet.ui.walletconnect

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.databinding.IdentityProofContainerBinding

class CredentialStatementAdapter(
    private val claims: List<IdentityProofRequestClaims>,
    private val onChangeAccountClicked: (index: Int) -> Unit,
    private val onIdentityChangeClicked: (index: Int) -> Unit,
) : RecyclerView.Adapter<CredentialStatementAdapter.ViewHolder>() {
    class ViewHolder(val containerBinding: IdentityProofContainerBinding) :
        RecyclerView.ViewHolder(containerBinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = IdentityProofContainerBinding.inflate(LayoutInflater.from(parent.context))
        binding.root.layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.MATCH_PARENT
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return claims.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val selectedCredential = claims[position].selectedCredential
        val identity = selectedCredential.identity

        holder.containerBinding.statements.setStatement(claims[position], identity)

        when (selectedCredential) {
            is IdentityProofRequestSelectedCredential.Account -> {
                with(holder.containerBinding.selectedCredentialInclude) {
                    accAddress.text = selectedCredential.account.getAccountName()
                    accBalance.isVisible = false
                    accIdentity.isVisible = true
                    accIdentity.text = identity.name
                }
                holder.containerBinding.selectedCredentialInclude.root.setOnClickListener {
                    onChangeAccountClicked(position)
                }
            }

            is IdentityProofRequestSelectedCredential.Identity -> {
                with(holder.containerBinding.selectedCredentialInclude) {
                    accAddress.text = identity.name
                    accBalance.isVisible = false
                    accIdentity.isVisible = false
                }
                holder.containerBinding.selectedCredentialInclude.root.setOnClickListener{
                    onIdentityChangeClicked(position)
                }
            }
        }
    }
}
