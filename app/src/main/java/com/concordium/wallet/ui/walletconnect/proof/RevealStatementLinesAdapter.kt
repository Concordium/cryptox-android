package com.concordium.wallet.ui.walletconnect.proof

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.concordium.sdk.crypto.wallet.web3Id.CredentialAttribute
import com.concordium.sdk.crypto.wallet.web3Id.Statement.RevealStatement
import com.concordium.wallet.data.util.IdentityAttributeConverterUtil
import com.concordium.wallet.databinding.IdentityProofStatementLineBinding


class RevealStatementLinesAdapter(private val context: Context,
                                  private val statements: List<RevealStatement>,
                                  private val attributes: Map<String, CredentialAttribute>
) :
    ArrayAdapter<RevealStatement>(context, 0, statements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding =
            if (convertView != null) IdentityProofStatementLineBinding.bind(
                convertView
            )
            else IdentityProofStatementLineBinding.inflate(
                LayoutInflater.from(
                    context
                ), parent, false
            )

        val statement = statements.get(position)

        val rawAttribute = attributes[statement.attributeTag]?.value ?: "???"
        // Assuming this is identity attributes
        val pair = IdentityAttributeConverterUtil.convertAttribute(context, Pair(statement.attributeTag,rawAttribute))
        binding.attributeTag.text = pair.first
        binding.attributeValue.text = pair.second

        return binding.root
    }
    }
