package com.concordium.wallet.ui.walletconnect.proof

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.concordium.sdk.crypto.wallet.web3Id.CredentialAttribute
import com.concordium.sdk.crypto.wallet.web3Id.Statement.RequestStatement
import com.concordium.sdk.crypto.wallet.web3Id.Statement.RevealStatement
import com.concordium.wallet.databinding.FragmentWalletConnectIdentityProofStatementsBinding

class DisplayStatements(context: Context, attr: AttributeSet): LinearLayout(context, attr) {
    var binding: FragmentWalletConnectIdentityProofStatementsBinding;

    init {
        binding = FragmentWalletConnectIdentityProofStatementsBinding.inflate(LayoutInflater.from(context))
        addView(binding.root)
    }

     fun setStatement(request: RequestStatement, attributes: Map<String, CredentialAttribute>) {

        val secretStatements = request.statement.filterNot { it is RevealStatement }
        val revealStatements = request.statement.filterIsInstance<RevealStatement>()

         binding.listView.adapter = SecretStatementAdapter(context, secretStatements)

         if (revealStatements.isEmpty()) {
             // If there are no reveal statements, then don't show the reveal box
             binding.revealStatements.root.visibility = View.GONE
         } else {
             binding.revealStatements.statementLines.adapter = RevealStatementLinesAdapter(context, revealStatements, attributes)
         }
    }

}
