package com.concordium.wallet.ui.cis2

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.ContractToken
import com.concordium.wallet.data.model.ProtocolLevelToken
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.IncludeTokenDetailsBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.plt.PLTInfoDialog
import com.concordium.wallet.ui.plt.PLTListInfoDialog
import com.concordium.wallet.uicore.view.ThemedCircularProgressDrawable
import com.concordium.wallet.util.PrettyPrint.asJsonString
import java.math.BigInteger

class TokenDetailsView(
    private val binding: IncludeTokenDetailsBinding,
    private val fragmentManager: FragmentManager,
) {

    private val context: Context
        get() = binding.root.context

    fun showTokenDetails(
        token: Token,
        isHideVisible: Boolean,
        onHideClicked: (() -> Unit)? = null,
    ) {
        setNameAndIcon(token)
        setTokenTypeLabel(token)
        setDescription(token)
        setDecimals(token)

        if (token is ContractToken) {
            setDisplay(token)
            setContractIndexAndSubIndex(token)
            setTokenId(token)
            setOwnership(token)
            setRawMetadataButton(token)
        }

        if (isHideVisible) {
            setHideButton(
                token = token,
                onClick = onHideClicked,
            )
        }
    }

    private fun setRawMetadataButton(token: ContractToken) {
        val metadata = token.metadata
        if (metadata != null) {
            binding.rawMetadataBtn.isVisible = true
            binding.rawMetadataBtn.setOnClickListener {
                RawMetadataDialog.newInstance(
                    RawMetadataDialog.setBundle(
                        rawMetadata = metadata.asJsonString()
                    )
                ).showSingle(fragmentManager, RawMetadataDialog.TAG)
            }
        }
    }

    private fun setTokenId(token: ContractToken) {
        // It is empty for the first token on the contract,
        // otherwise a hexadecimal counter.
        val tokenId =
            token
                .token
                .takeIf(String::isNotEmpty)
                ?: return

        binding.tokenIdHolder.visibility = View.VISIBLE
        binding.tokenId.text = tokenId
    }

    private fun setDescription(token: Token) {
        when (token) {
            is CCDToken -> {
                binding.descriptionHolder.visibility = View.VISIBLE
                binding.description.text = context.getString(R.string.ccd_details_description)
            }

            is ContractToken -> {
                val description =
                    token
                        .metadata
                        ?.description
                        ?.takeIf(String::isNotBlank)

                if (description != null) {
                    binding.descriptionHolder.visibility = View.VISIBLE
                    binding.description.text = description
                }
            }

            is ProtocolLevelToken -> {
                binding.descriptionHolder.visibility = View.GONE
            }
        }
    }

    private fun setOwnership(token: ContractToken) {
        if (token.metadata?.unique == true) {
            binding.ownershipHolder.visibility = View.VISIBLE
            binding.ownership.text =
                if (token.balance != BigInteger.ZERO)
                    context.getString(R.string.cis_owned)
                else
                    context.getString(R.string.cis_not_owned)
        }
    }

    private fun setDisplay(token: ContractToken) {
        val displayUrl =
            token
                .metadata
                ?.display
                ?.url
                ?.takeIf(String::isNotBlank)
                ?: return

        binding.display.isVisible = true
        Glide.with(context)
            .load(displayUrl)
            .placeholder(ThemedCircularProgressDrawable(context))
            .fitCenter()
            .into(binding.display)
    }

    private fun setNameAndIcon(token: Token) {
        binding.nameAndIconHolder.visibility = View.VISIBLE

        TokenIconView(binding.icon)
            .showTokenIcon(token)

        binding.name.text = when (token) {
            is CCDToken ->
                context.getString(R.string.account_details_ccd_token)

            is ContractToken ->
                token.metadata?.name ?: token.symbol

            is ProtocolLevelToken ->
                token.name ?: token.symbol
        }
    }

    private fun setContractIndexAndSubIndex(token: ContractToken) {
        val tokenIndex = token.contractIndex
        if (tokenIndex.isNotBlank()) {
            binding.contractIndexHolder.visibility = View.VISIBLE
            binding.contractIndex.text = token.contractIndex
            if (token.subIndex.isNotBlank()) {
                val combinedInfo = "${tokenIndex}, ${token.subIndex}"
                binding.contractIndex.text = combinedInfo
            } else {
                binding.contractIndex.text = tokenIndex
            }
        }
    }

    private fun setDecimals(token: Token) {
        if (token.decimals != 0) {
            binding.decimalsHolder.visibility = View.VISIBLE
            binding.decimals.text =
                context.getString(
                    R.string.account_token_details_decimals,
                    token.decimals.toString(),
                )
        }
    }

    private fun setHideButton(
        token: Token,
        onClick: (() -> Unit)?,
    ) {
        binding.hideToken.isVisible = token !is CCDToken
        binding.hideToken.setOnClickListener {
            onClick?.invoke()
        }
    }

    private fun setTokenTypeLabel(token: Token) {
        when (token) {
            is ProtocolLevelToken -> {
                binding.cis2TokenTypeHolder.rootLayout.visibility = View.GONE
                binding.pltListStatusHolder.visibility = View.VISIBLE
                binding.pltListStatus.setToken(
                    token = token,
                    onTokenLabelClick = {
                        PLTInfoDialog().showSingle(fragmentManager, PLTInfoDialog.TAG)
                    },
                    onTokenStatusClick = {
                        PLTListInfoDialog().showSingle(
                            fragmentManager,
                            PLTListInfoDialog.TAG
                        )
                    }
                )
            }

            is ContractToken -> {
                binding.cis2TokenTypeHolder.rootLayout.visibility = View.VISIBLE
                binding.pltListStatusHolder.visibility = View.GONE
                binding.cis2TokenTypeHolder.rootLayout.setOnClickListener {
                    CIS2InfoDialog().showSingle(fragmentManager, CIS2InfoDialog.TAG)
                }
            }

            else -> {
                binding.pltListStatusHolder.visibility = View.GONE
                binding.cis2TokenTypeHolder.rootLayout.visibility = View.GONE
            }
        }
    }
}
