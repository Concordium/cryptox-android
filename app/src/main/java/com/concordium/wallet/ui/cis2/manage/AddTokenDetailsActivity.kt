package com.concordium.wallet.ui.cis2.manage

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TokenMetadata
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

        initViews(requireNotNull(intent.getSerializable(TOKEN, Token::class.java)))
    }

    private fun initViews(token: Token) {
        binding.detailsLayout.hideToken.visibility = View.GONE
        setContractIndexAndSubIndex(token)
        setTokenId(token.token)
        token.metadata?.let { tokenMetadata ->
            setNameAndIcon(tokenMetadata, token.isUnique)
            setDescription(tokenMetadata)
            setTicker(tokenMetadata)
            setDecimals(token)
            setRawMetadataButton(tokenMetadata)
        }
    }

    private fun setTokenId(tokenId: String) {
        if (tokenId.isNotBlank()) {
            binding.detailsLayout.tokenIdHolder.visibility = View.VISIBLE
            binding.detailsLayout.tokenId.text = tokenId
        }
    }

    private fun setDescription(tokenMetadata: TokenMetadata) {
        if (!tokenMetadata.description.isNullOrBlank()) {
            binding.detailsLayout.descriptionHolder.visibility = View.VISIBLE
            binding.detailsLayout.description.text = tokenMetadata.description
        }
    }

    private fun setNameAndIcon(tokenMetadata: TokenMetadata, isUnique: Boolean) {
        val name = tokenMetadata.name
        val thumbnail = tokenMetadata.thumbnail?.url
        binding.detailsLayout.nameAndIconHolder.visibility = View.VISIBLE

        if (!thumbnail.isNullOrBlank()) {
            loadImage(binding.detailsLayout.icon, thumbnail)

            if (isUnique) {
                binding.detailsLayout.nftIcon.visibility = View.VISIBLE
                tokenMetadata.display?.url?.let { loadImage(binding.detailsLayout.nftIcon, it) }
            }
        } else {
            binding.detailsLayout.icon.setImageResource(R.drawable.ic_token_no_image)
        }
        binding.detailsLayout.name.text = name
    }

    private fun loadImage(view: AppCompatImageView, url: String) {
        Glide.with(view.context)
            .load(url)
            .placeholder(ThemedCircularProgressDrawable(view.context))
            .fitCenter()
            .into(view)
    }

    private fun setContractIndexAndSubIndex(token: Token) {
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

    private fun setTicker(tokenMetadata: TokenMetadata) {
        if (!tokenMetadata.symbol.isNullOrBlank()) {
            binding.detailsLayout.tokenHolder.visibility = View.VISIBLE
            binding.detailsLayout.token.text = tokenMetadata.symbol
        }
    }

    private fun setDecimals(token: Token) {
        if (!token.isUnique) {
            binding.detailsLayout.decimalsHolder.visibility = View.VISIBLE
            binding.detailsLayout.decimals.text = token.decimals.toString()
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