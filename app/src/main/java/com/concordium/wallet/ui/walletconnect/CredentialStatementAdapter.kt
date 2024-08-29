package com.concordium.wallet.ui.walletconnect

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.concordium.sdk.crypto.wallet.web3Id.Statement.RequestStatement
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.databinding.IdentityProofContainerBinding
import com.concordium.wallet.util.Log

class CredentialStatementAdapter(
    private val statements: List<RequestStatement>,
    private val accounts: List<Account>,
    private val getIdentity: (account: Account) -> Identity?,
    private val onChangeAccountClicked: (index: Int) -> Unit
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
        return statements.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val account = accounts[position]
        val identity = getIdentity(account)

        if (identity == null) {
            Log.e("Identity is not available for account ${account.address}")
            return
        }

        holder.containerBinding.statements.setStatement(statements[position], identity)
        with(holder.containerBinding.selectedAccountInclude) {
            accAddress.text = account.getAccountName()
            // TODO do we want to show amount here or identity?
            accBalance.text = root.context.getString(
                com.concordium.wallet.R.string.acc_balance_placeholder,
                com.concordium.wallet.data.util.CurrencyUtil.formatGTU(
                    account.balanceAtDisposal(),
                    true
                )
            )
        }
        holder.containerBinding.selectedAccountIncludeContainer.setOnClickListener {
            onChangeAccountClicked(position)
        }
    }
}
