package com.concordium.wallet.ui.walletconnect.proof

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.concordium.sdk.crypto.wallet.web3Id.CredentialAttribute
import com.concordium.sdk.crypto.wallet.web3Id.Statement.RequestStatement
import com.concordium.sdk.crypto.wallet.web3Id.Statement.RevealStatement
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentWalletConnectIdentityProofStatementsBinding
import com.fasterxml.jackson.databind.ObjectMapper

class DisplayStatements :
    Fragment(R.layout.fragment_wallet_connect_identity_proof_statements) {

    lateinit var binding: FragmentWalletConnectIdentityProofStatementsBinding;

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentWalletConnectIdentityProofStatementsBinding.bind(view)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val requestString = arguments?.getString("request")
        val request = ObjectMapper().readValue(requestString, RequestStatement::class.java)

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
