package com.concordium.wallet.ui.more.export

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.concordium.wallet.R
import com.concordium.wallet.data.model.AccountDataKeys
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityExportAccountKeysBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.util.getSerializable
import com.google.gson.Gson

class ExportAccountKeysActivity : BaseActivity(
    R.layout.activity_export_account_keys,
    R.string.export_account_keys_title,
), AuthDelegate by AuthDelegateImpl() {
    private lateinit var binding: ActivityExportAccountKeysBinding
    private val viewModel: ExportAccountKeysViewModel by viewModels()

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportAccountKeysBinding.bind(findViewById(R.id.root_layout))
        setContentView(binding.root)
        hideActionBarBack(isVisible = true)
        viewModel.account = intent.getSerializable(EXTRA_ACCOUNT, Account::class.java)
        initViews()
        initObservers()
    }

    private fun initViews() {
        binding.revealMessageTextView.text = getString(
            R.string.export_account_keys_reveal_message,
            viewModel.account.getAccountName()
        )
        binding.revealButton.setOnClickListener {
            reveal()
        }
        binding.copyButton.setOnClickListener {
            copyToClipboard()
        }
        binding.exportButton.setOnClickListener {
            openFolderPicker(getResultFolderPicker)
        }
        binding.done.setOnClickListener {
            finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initObservers() {
        viewModel.textResourceInt.observe(this) {
            Toast.makeText(this, getString(it), Toast.LENGTH_SHORT).show()
        }
        viewModel.accountData.observe(this) {
            binding.hidden.visibility = View.GONE
            binding.revealed.visibility = View.VISIBLE
            binding.done.visibility = View.VISIBLE
            viewModel.accountDataKeys = Gson().fromJson(it.keys.json, AccountDataKeys::class.java)
            binding.keyTextView.text = viewModel.accountDataKeys.level0.keys.keys.signKey
        }
    }

    private fun reveal() {
        showAuthentication(
            activity = this@ExportAccountKeysActivity,
            onAuthenticated = viewModel::continueWithPassword
        )
    }

    private fun copyToClipboard() {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData =
            ClipData.newPlainText("key", viewModel.accountDataKeys.level0.keys.keys.signKey)
        clipboardManager.setPrimaryClip(clipData)

        binding.copyButton.text = getString(R.string.setup_wallet_seed_phrase_copied)
        binding.copyButton.setCompoundDrawablesRelativeWithIntrinsicBounds(
            ContextCompat.getDrawable(this, R.drawable.cryptox_ico_check_16),
            null,
            null,
            null,
        )
    }

    private val getResultFolderPicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.data?.let { uri ->
                    viewModel.saveFileToLocalFolder(uri)
                }
            }
        }
}
