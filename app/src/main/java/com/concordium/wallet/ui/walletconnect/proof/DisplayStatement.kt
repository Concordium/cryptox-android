package com.concordium.wallet.ui.walletconnect.proof

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.concordium.sdk.crypto.wallet.web3Id.Statement.AtomicStatement
import com.concordium.sdk.crypto.wallet.web3Id.Statement.RequestStatement
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentWalletConnectIdentityProofStatementBinding
import com.fasterxml.jackson.databind.ObjectMapper

class DisplayStatement: Fragment (R.layout.fragment_wallet_connect_identity_proof_statement) {

    private lateinit var binding: FragmentWalletConnectIdentityProofStatementBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletConnectIdentityProofStatementBinding.inflate(inflater, container, true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val request = ObjectMapper().readValue(arguments?.getString("statement"), RequestStatement::class.java)
        // TODO change to handle multiple statements.
        val atomicStatement = request.statement[0];
        binding.attributeTag.text = atomicStatement.attributeTag
        binding.attributeValue.text = getStatementValue(atomicStatement)
        binding.statementDescription.text = getStatementDescription(atomicStatement)
    }

    private fun getStatementDescription(atomicStatement: AtomicStatement?): String {
        TODO("Not yet implemented")
    }

    private fun getStatementValue(atomicStatement: AtomicStatement?): String {
        TODO("Not yet implemented")
    }

    data class CreatedView(
        val view: FragmentWalletConnectIdentityProofStatementBinding,
        val lifecycleOwner: LifecycleOwner,
    )
}
