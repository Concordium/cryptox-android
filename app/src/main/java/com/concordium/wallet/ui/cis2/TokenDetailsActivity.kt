package com.concordium.wallet.ui.cis2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityTokenDetailsBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.account.accountqrcode.AccountQRCodeActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.view.ThemedCircularProgressDrawable
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.getSerializable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.math.BigInteger

class TokenDetailsActivity : BaseActivity(R.layout.activity_token_details) {
    private lateinit var binding: ActivityTokenDetailsBinding
    private val viewModel: TokensViewModel by viewModels()

    companion object {
        const val ACCOUNT = "ACCOUNT"
        const val TOKEN = "TOKEN"
        const val CHANGED = "CHANGED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTokenDetailsBinding.bind(findViewById(R.id.root_layout))
        initViews()
        initObservers()
    }

    private fun initViews() {
        viewModel.tokenData.account =
            requireNotNull(intent.getSerializable(ACCOUNT, Account::class.java)) {
                "Missing account extra"
            }
        viewModel.tokenData.selectedToken =
            requireNotNull(intent.getSerializable(TOKEN, Token::class.java)) {
                "Missing token extra"
            }

        Log.d("TOKEN : ${viewModel.tokenData}")
        Log.d("ACCOUNT : ${viewModel.tokenData.account}")

        val tokenName = viewModel.tokenData.selectedToken?.metadata?.name
        setActionBarTitle(
            getString(
                R.string.cis_token_details_title,
                tokenName,
                viewModel.tokenData.account?.getAccountName()
            )
        )
        hideActionBarBack(true)

        binding.includeButtons.send.isEnabled =
            viewModel.tokenData.account.let { it != null && !it.readOnly && it.transactionStatus == TransactionStatus.FINALIZED }
        binding.includeButtons.send.setOnClickListener {
            val intent = Intent(this, SendTokenActivity::class.java)
            intent.putExtra(SendTokenActivity.ACCOUNT, viewModel.tokenData.account)
            intent.putExtra(SendTokenActivity.TOKEN, viewModel.tokenData.selectedToken)
            intent.putExtra(
                SendTokenActivity.PARENT_ACTIVITY,
                AccountDetailsActivity::class.java.canonicalName
            )
            startActivityForResultAndHistoryCheck(intent)
        }
        binding.includeButtons.receive.setOnClickListener {
            val intent = Intent(this, AccountQRCodeActivity::class.java)
            intent.putExtra(AccountQRCodeActivity.EXTRA_ACCOUNT, viewModel.tokenData.account)
            startActivity(intent)
        }

        binding.includeAbout.deleteToken.setOnClickListener {
            showDeleteDialog()
        }

        viewModel.tokenData.selectedToken?.let { token ->
            setContractIndexAndSubIndex(token)
            setTokenId(token.token)
            setBalance(token)
            token.metadata?.let { tokenMetadata ->
                setNameAndIcon(tokenMetadata)
                setImage(tokenMetadata)
                setOwnership(token, tokenMetadata)
                setDescription(tokenMetadata)
                setTicker(tokenMetadata)
                setDecimals(token)
            }
            if (token.isNewlyReceived) {
                handleNewlyReceivedToken(token)
            }
        }
    }

    private fun handleNewlyReceivedToken(token: Token) {
        viewModel.unmarkNewlyReceivedSelectedToken()
        showNewlyReceivedNotice(
            tokenName = token.name ?: token.symbol
        )
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(CHANGED, true)
        )
    }

    private fun showDeleteDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.cis_delete_dialog_title)
        builder.setMessage(getString(R.string.cis_delete_dialog_content))
        builder.setPositiveButton(getString(R.string.cis_delete_dialog_confirm)) { dialog, _ ->
            dialog.dismiss()
            viewModel.deleteSelectedToken()
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(CHANGED, true)
            )
            finish()
        }
        builder.setNegativeButton(getString(R.string.cis_delete_dialog_cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun setBalance(token: Token) {
        binding.tokenAmount.text = CurrencyUtil.formatGTU(token.balance, token)
    }

    private fun setTokenId(tokenId: String) {
        if (tokenId.isNotBlank()) {
            binding.includeAbout.tokenIdHolder.visibility = View.VISIBLE
            binding.includeAbout.tokenId.text = tokenId
        }
    }

    private fun setDescription(tokenMetadata: TokenMetadata) {
        if (!tokenMetadata.description.isNullOrBlank()) {
            binding.includeAbout.descriptionHolder.visibility = View.VISIBLE
            binding.includeAbout.description.text = tokenMetadata.description
        }
    }

    private fun setOwnership(token: Token, tokenMetadata: TokenMetadata) {
        if (tokenMetadata.unique == true) {
            binding.includeAbout.ownershipHolder.visibility = View.VISIBLE
            binding.includeAbout.ownership.text =
                if (token.balance != BigInteger.ZERO)
                    getString(R.string.cis_owned)
                else
                    getString(R.string.cis_not_owned)
        }
    }

    private fun setNameAndIcon(tokenMetadata: TokenMetadata) {
        val name = tokenMetadata.name
        val thumbnail = tokenMetadata.thumbnail?.url
        binding.includeAbout.nameAndIconHolder.visibility = View.VISIBLE
        if (!thumbnail.isNullOrBlank()) {
            Glide.with(this)
                .load(thumbnail)
                .placeholder(ThemedCircularProgressDrawable(this))
                .fitCenter()
                .into(binding.includeAbout.icon)
        } else {
            binding.includeAbout.icon.setImageResource(R.drawable.ic_token_no_image)
        }
        binding.includeAbout.name.text = name
    }

    private fun setContractIndexAndSubIndex(token: Token) {
        val tokenIndex = token.contractIndex
        if (tokenIndex.isNotBlank()) {
            binding.includeAbout.contractIndexHolder.visibility = View.VISIBLE
            binding.includeAbout.contractIndex.text = token.contractIndex
            if (token.subIndex.isNotBlank()) {
                val combinedInfo = "${tokenIndex}, ${token.subIndex}"
                binding.includeAbout.contractIndex.text = combinedInfo
            } else {
                binding.includeAbout.contractIndex.text = tokenIndex
            }
        }
    }

    private fun setImage(tokenMetadata: TokenMetadata) {
        if (!tokenMetadata.display?.url.isNullOrBlank()) {
            binding.includeAbout.imageTitle.visibility = View.VISIBLE
            binding.includeAbout.image.visibility = View.VISIBLE
            Glide.with(this)
                .load(tokenMetadata.display?.url)
                .placeholder(ThemedCircularProgressDrawable(this))
                .fitCenter()
                .into(binding.includeAbout.image)
        } else {
            binding.includeAbout.imageTitle.visibility = View.GONE
            binding.includeAbout.image.visibility = View.GONE
        }
    }

    private fun setTicker(tokenMetadata: TokenMetadata) {
        if (!tokenMetadata.symbol.isNullOrBlank()) {
            binding.includeAbout.tokenHolder.visibility = View.VISIBLE
            binding.includeAbout.token.text = tokenMetadata.symbol
        }
    }

    private fun setDecimals(token: Token) {
        if (!token.isUnique) {
            binding.includeAbout.decimalsHolder.visibility = View.VISIBLE
            binding.includeAbout.decimals.text = token.decimals.toString()
        }
    }

    private fun showNewlyReceivedNotice(tokenName: String) {
        NewlyReceivedTokenNoticeDialog.newInstance(
            NewlyReceivedTokenNoticeDialog.getBundle(
                tokenName = tokenName,
            )
        ).showSingle(supportFragmentManager, NewlyReceivedTokenNoticeDialog.TAG)
    }

    private fun initObservers() {
        viewModel.waiting.observe(this) { waiting ->
            showWaiting(waiting)
        }

        supportFragmentManager.setFragmentResultListener(
            NewlyReceivedTokenNoticeDialog.ACTION_REQUEST,
            this,
        ) { _, bundle ->
            val isKeepingToken = NewlyReceivedTokenNoticeDialog.getResult(bundle)
            if (!isKeepingToken) {
                showDeleteDialog()
            }
        }
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressBar.visibility = if (waiting) View.VISIBLE else View.GONE
    }
}
