package com.concordium.wallet.ui.walletconnect.proof

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
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

     fun setStatement(request: RequestStatement) {

        val secretStatements = request.statement.filterNot { it is RevealStatement }
        val revealStatements = request.statement.filterIsInstance<RevealStatement>()

         binding.listView.adapter = SecretStatementAdapter(context, secretStatements)

         binding.revealStatements.statementLines.adapter = RevealStatementLinesAdapter(context, revealStatements, object: Map<String, CredentialAttribute> {
            override fun get(key: String): CredentialAttribute? {
                return CredentialAttribute.builder().type(CredentialAttribute.CredentialAttributeType.STRING).value("Age").build()
            }

            override val entries: Set<Map.Entry<String, CredentialAttribute>>
                get() = TODO("Not yet implemented")
            override val keys: Set<String>
                get() = TODO("Not yet implemented")
            override val size: Int
                get() = TODO("Not yet implemented")
            override val values: Collection<CredentialAttribute>
                get() = TODO("Not yet implemented")

            override fun isEmpty(): Boolean {
                TODO("Not yet implemented")
            }

            override fun containsValue(value: CredentialAttribute): Boolean {
                TODO("Not yet implemented")
            }

            override fun containsKey(key: String): Boolean {
                TODO("Not yet implemented")
            }
        })
    }

}
