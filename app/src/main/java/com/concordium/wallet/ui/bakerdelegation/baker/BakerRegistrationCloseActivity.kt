package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_KEYS
import com.concordium.wallet.databinding.ActivityBakerRegistrationCloseBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.bakerdelegation.dialog.baker.BakerNoticeDialog

class BakerRegistrationCloseActivity : BaseDelegationBakerActivity(
    R.layout.activity_baker_registration_close, R.string.baker_registration_validator_keys_title
) {
    private lateinit var binding: ActivityBakerRegistrationCloseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBakerRegistrationCloseBinding.bind(findViewById(R.id.root_layout))
        hideActionBarBack(isVisible = true)
        initViews()
        generateKeys()
    }

    override fun initViews() {
        if (viewModel.bakerDelegationData.type == UPDATE_BAKER_KEYS)
            setActionBarTitle(R.string.baker_update_keys_settings_title)

        binding.bakerRegistrationExport.setOnClickListener {
            startExport()
        }

        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                Toast.makeText(baseContext, getString(value), Toast.LENGTH_SHORT).show()
            }
        })

        viewModel.bakerKeysLiveData.observe(this) { bakerKeys ->
            binding.bakerRegistrationExportElectionVerifyKey.text =
                bakerKeys?.electionVerifyKey ?: ""
            binding.bakerRegistrationExportSignatureVerifyKey.text =
                bakerKeys?.signatureVerifyKey ?: ""
            binding.bakerRegistrationExportAggregationVerifyKey.text =
                bakerKeys?.aggregationVerifyKey ?: ""
        }

        viewModel.fileSavedLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                Toast.makeText(baseContext, getString(value), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun generateKeys() {
        viewModel.generateKeys()
        showNotice()
    }

    private fun showNotice() {
        BakerNoticeDialog.newInstance(
            BakerNoticeDialog.setBundle(
                noticeMessage = getString(R.string.baker_registration_export_notice_message),
                redirectToMainActivity = false
            )
        ).showSingle(supportFragmentManager, BakerNoticeDialog.TAG)
    }

    private fun startExport() {
        openFolderPicker(getResultFolderPicker)
    }

    private val getResultFolderPicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.data?.let { uri ->
                    viewModel.saveFileToLocalFolder(uri)
                    continueToBakerConfirmation()
                }
            }
        }

    private fun continueToBakerConfirmation() {
        val intent = Intent(this, BakerRegistrationConfirmationActivity::class.java)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }

    override fun errorLiveData(value: Int) {
    }
}
