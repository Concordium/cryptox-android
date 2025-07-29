package com.concordium.wallet.ui.cis2.manage

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.NewContractToken
import com.concordium.wallet.data.model.NewToken
import com.concordium.wallet.data.model.PLTToken
import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityAddTokenDetailsBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.RawMetadataDialog
import com.concordium.wallet.uicore.view.ThemedCircularProgressDrawable
import com.concordium.wallet.util.PrettyPrint.asJsonString
import com.concordium.wallet.util.getSerializable

class AddTokenDetailsActivity : BaseActivity(
    R.layout.activity_add_token_details,
    R.string.cis_add_token_title
) {

    private val binding by lazy {
        ActivityAddTokenDetailsBinding.bind(findViewById(R.id.root))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(isVisible = true)

        initViews(requireNotNull(intent.getSerializable(TOKEN, NewToken::class.java)))
    }

    private fun initViews(token: NewToken) {
        binding.detailsLayout.hideToken.visibility = View.GONE
        setContractIndexAndSubIndex(token)
        setTokenId(token)
        setPLTListStatus(token)
        token.metadata?.let { tokenMetadata ->
            setNameAndIcon(token)
            setDescription(tokenMetadata)
            setTicker(tokenMetadata)
            setDecimals(token)
            setTotalSupply(token)
            setRawMetadataButton(tokenMetadata)
        }
    }

    private fun setTokenId(token: NewToken) {
        if (token is NewContractToken) {
            if (token.token.isNotBlank()) {
                binding.detailsLayout.tokenIdHolder.visibility = View.VISIBLE
                binding.detailsLayout.tokenId.text = token.token
            }
        }
    }

    private fun setDescription(tokenMetadata: TokenMetadata) {
        if (!tokenMetadata.description.isNullOrBlank()) {
            binding.detailsLayout.descriptionHolder.visibility = View.VISIBLE
            binding.detailsLayout.description.text = tokenMetadata.description
        }
    }

    private fun setNameAndIcon(token: NewToken) {
        val name = token.metadata?.name ?: token.symbol
        val thumbnail = token.metadata?.thumbnail?.url
        binding.detailsLayout.nameAndIconHolder.visibility = View.VISIBLE

        if (!thumbnail.isNullOrBlank()) {
            loadImage(binding.detailsLayout.icon, thumbnail)

            if (token.metadata?.unique == true) {
                binding.detailsLayout.nftIcon.visibility = View.VISIBLE
                token.metadata?.display?.url?.let { loadImage(binding.detailsLayout.nftIcon, it) }
            }
        } else {
            binding.detailsLayout.icon.setImageResource(R.drawable.mw24_ic_token_placeholder)
        }
        binding.detailsLayout.name.text = name
    }

    private fun loadImage(view: AppCompatImageView, url: String) {
        Glide.with(view.context)
            .load(url)
            .placeholder(ThemedCircularProgressDrawable(view.context))
            .error(R.drawable.mw24_ic_token_placeholder)
            .fitCenter()
            .into(view)
    }

    private fun setContractIndexAndSubIndex(token: NewToken) {
        if (token is NewContractToken) {
            val tokenIndex = token.contractIndex

            if (tokenIndex.isNotBlank()) {
                binding.detailsLayout.contractIndexHolder.visibility = View.VISIBLE
                binding.detailsLayout.contractIndex.text = token.contractIndex
                if (token.subIndex.isNotBlank()) {
                    val combinedInfo = "${tokenIndex}, ${token.subIndex}"
                    binding.detailsLayout.contractIndex.text = combinedInfo
                } else {
                    binding.detailsLayout.contractIndex.text = tokenIndex
                }
            }
        }
    }

    private fun setTicker(tokenMetadata: TokenMetadata) {
        if (!tokenMetadata.symbol.isNullOrBlank()) {
            binding.detailsLayout.tokenHolder.visibility = View.VISIBLE
            binding.detailsLayout.token.text = tokenMetadata.symbol
        }
    }

    private fun setDecimals(token: NewToken) {
        if (token.metadata?.unique?.not() == true) {
            binding.detailsLayout.decimalsHolder.visibility = View.VISIBLE
            binding.detailsLayout.decimals.text = token.decimals.toString()
        }
    }

    private fun setPLTListStatus(token: NewToken) {
        if (token is PLTToken) {
            binding.detailsLayout.pltListStatusHolder.visibility = View.VISIBLE
            binding.detailsLayout.pltListStatus.setToken(token)
        } else {
            binding.detailsLayout.pltListStatusHolder.visibility = View.GONE
        }
    }

    private fun setTotalSupply(token: NewToken) {
        if (token is PLTToken) {
            token.metadata?.totalSupply?.let {
                binding.detailsLayout.totalSupplyHolder.visibility = View.VISIBLE
                binding.detailsLayout.supply.text = CurrencyUtil.formatGTU(
                    it,
                    token.metadata?.decimals ?: 0
                )
            } ?: run {
                binding.detailsLayout.totalSupplyHolder.visibility = View.GONE
            }
        }
    }

    private fun setRawMetadataButton(tokenMetadata: TokenMetadata) {
        binding.detailsLayout.rawMetadataBtn.setOnClickListener {
            RawMetadataDialog.newInstance(
                RawMetadataDialog.setBundle(rawMetadata = tokenMetadata.asJsonString())
            ).showSingle(supportFragmentManager, RawMetadataDialog.TAG)
        }
    }

    companion object {
        const val TOKEN = "token_details"
    }

}