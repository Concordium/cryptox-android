package com.concordium.wallet.ui.walletconnect

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.concordium.sdk.crypto.wallet.identityobject.AttributeList
import com.concordium.sdk.crypto.wallet.identityobject.IdentityObject
import com.concordium.sdk.crypto.wallet.web3Id.CredentialAttribute
import com.concordium.sdk.crypto.wallet.web3Id.Statement.AtomicStatement
import com.concordium.sdk.crypto.wallet.web3Id.Statement.MembershipStatement
import com.concordium.sdk.crypto.wallet.web3Id.Statement.NonMembershipStatement
import com.concordium.sdk.crypto.wallet.web3Id.Statement.RangeStatement
import com.concordium.sdk.crypto.wallet.web3Id.Statement.RequestStatement
import com.concordium.sdk.crypto.wallet.web3Id.Statement.RevealStatement
import com.concordium.sdk.responses.accountinfo.credential.AttributeType
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.util.IdentityAttributeConverterUtil
import com.concordium.wallet.databinding.FragmentWalletConnectIdentityProofStatementCardBinding
import com.concordium.wallet.databinding.FragmentWalletConnectIdentityProofStatementsBinding
import com.concordium.wallet.databinding.IdentityProofStatementLineBinding

class DisplayStatements(context: Context, attrs: AttributeSet): LinearLayout(context, attrs) {
    var binding: FragmentWalletConnectIdentityProofStatementsBinding;

    init {
        binding = FragmentWalletConnectIdentityProofStatementsBinding.inflate(LayoutInflater.from(context))
        binding.revealStatements.statementHeader.setText(R.string.information_to_reveal)
        binding.revealStatements.statementDescription.setText(R.string.reveal_description)
        addView(binding.root)
    }

     fun setStatement(request: RequestStatement, identity: Identity) {

        val secretStatements = request.statement.filterNot { it is RevealStatement }
        val revealStatements = request.statement.filterIsInstance<RevealStatement>()

         val attributes = identity.identityObject?.let { it.attributeList.chosenAttributes.mapValues { attribute ->
             CredentialAttribute.builder().value(attribute.value)
                 .type(CredentialAttribute.CredentialAttributeType.STRING).build()
         }} ?: emptyMap()

         secretStatements.forEach {
             binding.root.addView(getSecretStatement(it, it.canBeProvedBy(getIdentityObject(identity))))
         }

         if (revealStatements.isEmpty()) {
             // If there are no reveal statements, then don't show the reveal box
             binding.revealStatements.root.visibility = View.GONE
         } else {
             revealStatements.forEach {
                 binding.revealStatements.statementLines.addView(getRevealLine(it, attributes))
             }
         }
    }

    private fun getStatementLine(tag: String, value: String, provable: Boolean, ): View {
        val revealBinding =
            IdentityProofStatementLineBinding.inflate(
                LayoutInflater.from(
                    context
                ), binding.root, false
            )

        revealBinding.attributeTag.text = tag
        revealBinding.attributeValue.text = value

        if (!provable) {
            revealBinding.checkMark.setImageResource(R.drawable.ccx_ico_declined)
        }

        return revealBinding.root
    }

    private fun getRevealLine(statement: RevealStatement, attributes: Map<String, CredentialAttribute>): View {
        val revealBinding =
            IdentityProofStatementLineBinding.inflate(
                LayoutInflater.from(
                    context
                ), binding.root, false
            )

        val rawAttribute = attributes[statement.attributeTag]?.value ?: "???"
        // Assuming this is identity attributes
        val pair = IdentityAttributeConverterUtil.convertAttribute(context, Pair(statement.attributeTag,rawAttribute))
        revealBinding.attributeTag.text = pair.first
        revealBinding.attributeValue.text = pair.second

        return revealBinding.root
    }

    private fun getSecretStatement(statement: AtomicStatement, provable: Boolean): View {
        val secretBinding =
            FragmentWalletConnectIdentityProofStatementCardBinding.inflate(
                LayoutInflater.from(
                    context
                ), binding.root, false
            )

        secretBinding.statementHeader.setText(R.string.zero_knowledge_proof)
        secretBinding.statementDescription.text = getStatementDescription(statement)
        val statementLine = getStatementLine(getPropertyTitle(statement.attributeTag), getStatementValue(statement), provable)
        secretBinding.statementLines.addView(statementLine)

        return secretBinding.root
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

/**
 * Get an IdentityObject compatible with the Concordium SDK methods.
 * N.B. Only the attributeList is populated, the remaining fields are null
  */
fun getIdentityObject(identity: Identity): IdentityObject {
    val identityObject = identity.identityObject!!
    val attributes = AttributeList.builder().chosenAttributes(identityObject.attributeList.chosenAttributes.mapKeys { AttributeType.fromJSON(it.key) }).build()
    return IdentityObject.builder().attributeList(attributes).build()
}
