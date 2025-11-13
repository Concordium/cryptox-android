package com.concordium.wallet.ui.more.export

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityExportTransactionLogBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.handleUrlClicks
import com.concordium.wallet.uicore.toast.showCustomToast
import com.concordium.wallet.util.getSerializable

class ExportTransactionLogActivity : BaseActivity(
    R.layout.activity_export_transaction_log,
    R.string.export_transaction_log_title
) {
    private lateinit var binding: ActivityExportTransactionLogBinding
    private val viewModel: ExportTransactionLogViewModel by viewModels()

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportTransactionLogBinding.bind(findViewById(R.id.toastLayoutTopError))
        viewModel.account = intent.getSerializable(EXTRA_ACCOUNT, Account::class.java)
        initViews()
        initObservers()
        viewModel.onIdleRequested()

        hideActionBarBack(isVisible = true) {
            finish()
        }
    }

    private fun initViews() {
        binding.notice.handleUrlClicks { url ->
            if (url == "#ccdscan") {
                val browserIntent =
                    Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.getExplorerUrl()))
                ContextCompat.startActivity(this, browserIntent, null)
            }
        }

        binding.generate.setOnClickListener {
            openFolderPicker(getResultFolderPicker)
        }

        binding.cancel.setOnClickListener {
            viewModel.onIdleRequested()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initObservers() {
        viewModel.downloadState.observe(this) { downloadState ->
            binding.notice.isVisible = downloadState is FileDownloadScreenState.Idle

            binding.downloadProgressLayout.isVisible =
                downloadState is FileDownloadScreenState.Downloading
            binding.downloadProgress.progress =
                when (downloadState) {
                    is FileDownloadScreenState.Downloading ->
                        downloadState.progress

                    else ->
                        0
                }
            binding.progressTextView.text = getString(
                R.string.export_transaction_log_progress,
                binding.downloadProgress.progress
            )

            binding.generate.isVisible = downloadState is FileDownloadScreenState.Idle
            binding.cancel.isVisible = downloadState is FileDownloadScreenState.Downloading
        }

        viewModel.events.collectWhenStarted(this) { event ->
            when (event) {
                Event.FinishWithNoContent -> {
                    showCustomToast(
                        iconResId = R.drawable.mw24_ic_circled_warning_exclamation,
                        title = getString(R.string.export_transaction_log_no_content)
                    )
                    finish()
                }

                Event.FinishWithSuccess -> {
                    showCustomToast(
                        title = getString(R.string.export_transaction_log_saved)
                    )
                    finish()
                }

                is Event.ShowError -> {
                    showError(event.resId)
                }
            }
        }
    }

    private val getResultFolderPicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.data?.let { destinationFolder ->
                    viewModel.downloadFile(destinationFolder)
                }
            }
        }
}
