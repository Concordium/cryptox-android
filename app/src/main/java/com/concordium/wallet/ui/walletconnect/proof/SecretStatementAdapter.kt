package com.concordium.wallet.ui.walletconnect.proof

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.concordium.sdk.crypto.wallet.web3Id.Statement.AtomicStatement
import com.concordium.sdk.crypto.wallet.web3Id.Statement.MembershipStatement
import com.concordium.sdk.crypto.wallet.web3Id.Statement.NonMembershipStatement
import com.concordium.sdk.crypto.wallet.web3Id.Statement.RangeStatement
import com.concordium.wallet.data.util.IdentityAttributeConverterUtil
import com.concordium.wallet.databinding.FragmentWalletConnectIdentityProofSecretStatementBinding


class SecretStatementAdapter(private var context: Context,
                             private var statements: List<AtomicStatement>
) :
    ArrayAdapter<AtomicStatement>(context, 0, statements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding =
            if (convertView != null) FragmentWalletConnectIdentityProofSecretStatementBinding.bind(
                convertView
            )
            else FragmentWalletConnectIdentityProofSecretStatementBinding.inflate(
                LayoutInflater.from(
                    context
                ), parent, false
            )

        val statement = statements[position]
        binding.statementLine.attributeTag.text = getPropertyTitle(statement.attributeTag)
        binding.statementLine.attributeValue.text = getStatementValue(statement)
        binding.statementDescription.text = getStatementDescription(statement)

        return binding.root
    }

    private fun getStatementDescription(atomicStatement: AtomicStatement): String {
        val name = getPropertyTitle(atomicStatement.attributeTag)
        return when (atomicStatement) {
            is RangeStatement -> "This will prove that your $name is between ${getPropertyValue(atomicStatement.attributeTag, atomicStatement.lower.value)} and ${getPropertyValue(atomicStatement.attributeTag, atomicStatement.upper.value)}"
            is MembershipStatement -> "This will prove that your $name is 1 of the following: ${atomicStatement.set.joinToString { getPropertyValue(atomicStatement.attributeTag, it.value) }}"
            is NonMembershipStatement -> "This will prove that your $name is none of the following: ${atomicStatement.set.joinToString { getPropertyValue(atomicStatement.attributeTag, it.value) }}"
            else -> throw IllegalArgumentException("Unknown statement type")
        }
    }

    private fun getPropertyTitle(attributeTag: String): String {
        return IdentityAttributeConverterUtil.convertAttributeTag(context, attributeTag)
    }

    private fun getPropertyValue(attributeTag: String, attributeValue: String): String {
        return IdentityAttributeConverterUtil.convertAttributeValue(context, attributeTag, attributeValue)
    }

    private fun getStatementValue(atomicStatement: AtomicStatement?): String {
        return when (atomicStatement) {
            is RangeStatement -> "between ${getPropertyValue(atomicStatement.attributeTag,atomicStatement.lower.value)} and ${getPropertyValue(atomicStatement.attributeTag, atomicStatement.upper.value)}"
            is MembershipStatement -> "1 of the following"
            is NonMembershipStatement -> "none of the following"
            else -> throw IllegalArgumentException("Unknown statement type")
        }
    }
}
