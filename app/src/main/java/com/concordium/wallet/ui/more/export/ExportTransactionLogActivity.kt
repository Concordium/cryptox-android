package com.concordium.wallet.ui.more.export

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityExportTransactionLogBinding
import com.concordium.wallet.ui.base.BaseActivity
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
        binding = ActivityExportTransactionLogBinding.bind(findViewById(R.id.root_layout))
        viewModel.account = intent.getSerializable(EXTRA_ACCOUNT, Account::class.java)
        initViews()
        initObservers()
        viewModel.onIdleRequested()

        hideActionBarBack(isVisible = true) {
            viewModel.onIdleRequested()
            finish()
        }
    }

    private fun initViews() {
        binding.description.movementMethod = LinkMovementMethod.getInstance()
        binding.description.autoLinkMask = Linkify.WEB_URLS
        binding.description.text = getString(
            R.string.export_transaction_log_description,
            viewModel.getExplorerUrl()
        )
        binding.generate.setOnClickListener {
            openFolderPicker(getResultFolderPicker)
        }
        binding.cancel.setOnClickListener {
            viewModel.onIdleRequested()
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
        viewModel.downloadState.observe(this) { downloadState ->
            binding.description.isVisible = downloadState is FileDownloadScreenState.Idle
            binding.notice.isVisible = downloadState is FileDownloadScreenState.Idle

            binding.downloadProgressLayout.isVisible =
                downloadState is FileDownloadScreenState.Downloading
            binding.downloadProgress.progress =
                when (downloadState) {
                    is FileDownloadScreenState.Downloading ->
                        downloadState.progress

                    is FileDownloadScreenState.Downloaded ->
                        100

                    else ->
                        0
                }
            binding.progressTextView.text = getString(
                R.string.export_transaction_log_progress,
                binding.downloadProgress.progress
            )

            binding.successLayout.isVisible = downloadState is FileDownloadScreenState.Downloaded
            binding.failedLayout.isVisible = downloadState is FileDownloadScreenState.Failed

            binding.generate.isVisible = downloadState is FileDownloadScreenState.Idle
            binding.cancel.isVisible = downloadState is FileDownloadScreenState.Downloading
            binding.done.isVisible = downloadState is FileDownloadScreenState.Downloaded
                    || downloadState is FileDownloadScreenState.Failed
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
