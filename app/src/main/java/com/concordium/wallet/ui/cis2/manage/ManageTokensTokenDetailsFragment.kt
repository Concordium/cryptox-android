package com.concordium.wallet.ui.cis2.manage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.databinding.FragmentManageTokensTokenDetailsBinding
import com.concordium.wallet.ui.cis2.TokensViewModel
import com.concordium.wallet.uicore.view.ThemedCircularProgressDrawable

class ManageTokensTokenDetailsFragment : Fragment() {
    private var _binding: FragmentManageTokensTokenDetailsBinding? = null
    private val binding get() = _binding!!
    private val _viewModel: TokensViewModel
        get() = (parentFragment as ManageTokensBottomSheet).viewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageTokensTokenDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.details.deleteToken.visibility = View.GONE
        binding.backToList.setOnClickListener {
            _viewModel.stepPage(-1)
        }
        _viewModel.chooseTokenInfo.observe(viewLifecycleOwner) { token ->
            setContractIndexAndSubIndex(token)
            setTokenId(token.token)
            token.metadata?.let { tokenMetadata ->
                setNameAndIcon(tokenMetadata)
                setImage(tokenMetadata)
                setDescription(tokenMetadata)
                setTicker(tokenMetadata)
                setDecimals(token)
            }
        }
    }

    private fun setTokenId(tokenId: String) {
        if (tokenId.isNotBlank()) {
            binding.details.tokenIdHolder.visibility = View.VISIBLE
            binding.details.tokenId.text = tokenId
        }
    }

    private fun setDescription(tokenMetadata: TokenMetadata) {
        if (!tokenMetadata.description.isNullOrBlank()) {
            binding.details.descriptionHolder.visibility = View.VISIBLE
            binding.details.description.text = tokenMetadata.description
        }
    }

    private fun setNameAndIcon(tokenMetadata: TokenMetadata) {
        val name = tokenMetadata.name
        val thumbnail = tokenMetadata.thumbnail?.url
        binding.details.nameAndIconHolder.visibility = View.VISIBLE

        if (!thumbnail.isNullOrBlank()) {
            Glide.with(this)
                .load(thumbnail)
                .placeholder(ThemedCircularProgressDrawable(binding.details.icon.context))
                .fitCenter()
                .into(binding.details.icon)
        } else if (thumbnail == "none") {
            binding.details.icon.setImageResource(R.drawable.ic_token_no_image)
        }
        binding.details.name.text = name
    }

    private fun setContractIndexAndSubIndex(token: Token) {
        val tokenIndex = token.contractIndex

        if (tokenIndex.isNotBlank()) {
            binding.details.contractIndexHolder.visibility = View.VISIBLE
            binding.details.contractIndex.text = token.contractIndex
            if (token.subIndex.isNotBlank()) {
                val combinedInfo = "${tokenIndex}, ${token.subIndex}"
                binding.details.contractIndex.text = combinedInfo
            } else {
                binding.details.contractIndex.text = tokenIndex
            }
        }
    }

    private fun setImage(tokenMetadata: TokenMetadata) {
        if (!tokenMetadata.display?.url.isNullOrBlank()) {
            binding.details.imageTitle.visibility = View.VISIBLE
            binding.details.image.visibility = View.VISIBLE

            Glide.with(this)
                .load(tokenMetadata.display?.url)
                .placeholder(ThemedCircularProgressDrawable(binding.details.image.context))
                .fitCenter()
                .into(binding.details.image)
        } else {
            binding.details.imageTitle.visibility = View.GONE
            binding.details.image.visibility = View.GONE
        }
    }

    private fun setTicker(tokenMetadata: TokenMetadata) {
        if (!tokenMetadata.symbol.isNullOrBlank()) {
            binding.details.tokenHolder.visibility = View.VISIBLE
            binding.details.token.text = tokenMetadata.symbol
        }
    }

    private fun setDecimals(token: Token) {
        if (!token.isUnique) {
            binding.details.decimalsHolder.visibility = View.VISIBLE
            binding.details.decimals.text = token.decimals.toString()
        }
    }
}
