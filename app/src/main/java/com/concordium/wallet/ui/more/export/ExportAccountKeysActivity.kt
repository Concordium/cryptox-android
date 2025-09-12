package com.concordium.wallet.ui.more.export

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityExportAccountKeysBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.uicore.toast.showCustomToast
import com.concordium.wallet.util.getSerializable

class ExportAccountKeysActivity : BaseActivity(
    R.layout.activity_export_account_keys,
    R.string.export_account_keys_title,
), AuthDelegate by AuthDelegateImpl() {

    private lateinit var binding: ActivityExportAccountKeysBinding
    private val viewModel: ExportAccountKeysViewModel by viewModels()
    private val folderPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.data?.let { uri ->
                    viewModel.saveFileToLocalFolder(uri)
                }
            }
        }

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportAccountKeysBinding.bind(findViewById(R.id.toastLayoutTopError))
        setContentView(binding.root)
        hideActionBarBack(isVisible = true)
        viewModel.account = intent.getSerializable(EXTRA_ACCOUNT, Account::class.java)
        initViews()
        initObservers()
    }

    private fun initViews() {
        binding.accountNameTextView.text = viewModel.account.getAccountName()

        binding.keyLayout.setOnClickListener {
            if (viewModel.revealedKeys.value == null) {
                reveal()
            }
        }

        binding.exportButton.setOnClickListener {
            exportFile()
        }

        binding.copyButton.setOnClickListener {
            copyToClipboard()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initObservers() {
        viewModel.revealedKeys.observe(this) { revealedKeys ->
            binding.revealedKeyTextView.isInvisible = revealedKeys == null
            binding.revealedKeyTextView.text = revealedKeys?.level0?.keys?.keys?.signKey

            binding.revealIcon.isInvisible = revealedKeys != null
            binding.revealTextView.isInvisible = revealedKeys != null
            binding.accountNameTextView.isInvisible = revealedKeys != null

            binding.copyButton.isVisible = revealedKeys != null
            binding.exportButton.isVisible = revealedKeys != null
        }

        viewModel.toastInt.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showCustomToast(
                    title = getString(value)
                )
            }
        })

        viewModel.errorInt.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
    }

    private fun reveal() {
        showAuthentication(
            activity = this@ExportAccountKeysActivity,
            onAuthenticated = viewModel::continueWithPassword
        )
    }

    private fun copyToClipboard() {
        viewModel.copyToClipboard()
    }

    private fun exportFile() {
        openFolderPicker(folderPickerLauncher)
    }
}
