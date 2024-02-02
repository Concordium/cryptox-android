package com.concordium.wallet.ui.recipient.recipient

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.databinding.ActivityRecipientBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.recipient.scanqr.ScanQRActivity
import com.concordium.wallet.ui.transaction.sendfunds.SendFundsActivity
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.KeyboardUtil

class RecipientActivity : BaseActivity(R.layout.activity_recipient, R.string.recipient_new_title) {

    companion object {
        const val EXTRA_RECIPIENT = "EXTRA_RECIPIENT"
        const val EXTRA_SELECT_RECIPIENT_MODE = "EXTRA_SELECT_RECIPIENT_MODE"
        const val EXTRA_GOTO_SCAN_QR = "EXTRA_GOTO_SCAN_QR"
        const val REQUEST_CODE_SCAN_QR = 2000
    }

    private lateinit var viewModel: RecipientViewModel
    private val binding by lazy {
        ActivityRecipientBinding.bind(findViewById(R.id.root_layout))
    }

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recipient = intent.getSerializableExtra(EXTRA_RECIPIENT) as Recipient?
        val selectRecipientMode = intent.getBooleanExtra(EXTRA_SELECT_RECIPIENT_MODE, false)

        initializeViewModel()
        viewModel.initialize(recipient, selectRecipientMode)
        initViews()

        val gotoScanQR = intent.getBooleanExtra(EXTRA_GOTO_SCAN_QR, false)
        if (gotoScanQR) {
            gotoScanBarCode()
        }

        hideActionBarBack(isVisible = true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SCAN_QR) {
            if (resultCode == Activity.RESULT_OK) {
                data?.getStringExtra(ScanQRActivity.EXTRA_BARCODE)?.let { barcode ->
                    showAddress(barcode)
                }
            }
        }
    }

    //endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[RecipientViewModel::class.java]

        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModel.errorLiveData.observe(
            this, object : EventObserver<Int>() {
                override fun onUnhandledEvent(value: Int) {
                    showErrorMessage(value)
                }
            }
        )
        viewModel.finishScreenLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    finish()
                }
            }
        })
        viewModel.gotoBackToSendFundsLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    goBackToSendFunds(viewModel.recipient)
                }
            }
        })
    }

    private fun initViews() {
        showWaiting(false)
        binding.saveButton.isEnabled = false
        if (viewModel.editRecipientMode) {
            setActionBarTitle(R.string.recipient_edit_title)
        }
        binding.recipientNameEdittext.afterTextChanged {
            binding.saveButton.isEnabled = validateRecipient()
        }
        binding.recipientAddressEdittext.afterTextChanged {
            binding.saveButton.isEnabled = validateRecipient()
        }
        binding.saveButton.setOnClickListener {
            saveRecipient()
        }
        binding.qrImageview.setOnClickListener {
            gotoScanBarCode()
        }
        // Setting these after the text changed listeners have been added above
        // To trigger initial validation and enabling the button
        binding.recipientNameEdittext.setText(viewModel.recipient.name)
        binding.recipientAddressEdittext.setText(viewModel.recipient.address)
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

    private fun showErrorMessage(stringRes: Int) {
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(binding.root, stringRes)
    }

    private fun validateRecipient(): Boolean {
        return viewModel.validateRecipient(
            binding.recipientNameEdittext.text.toString(),
            binding.recipientAddressEdittext.text.toString()
        )
    }

    private fun saveRecipient() {
        val isSaving = viewModel.validateAndSaveRecipient(
            binding.recipientNameEdittext.text.toString(),
            binding.recipientAddressEdittext.text.toString()
        )
        if (isSaving) {
            binding.saveButton.isEnabled = false
            KeyboardUtil.hideKeyboard(this)
        }
    }

    private fun goBackToSendFunds(recipient: Recipient) {
        val intent = Intent(this, SendFundsActivity::class.java)
        intent.putExtra(SendFundsActivity.EXTRA_RECIPIENT, recipient)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    private fun gotoScanBarCode() {
        val intent = Intent(this, ScanQRActivity::class.java)
        intent.putExtra("add_contact", true)
        startActivityForResult(intent, REQUEST_CODE_SCAN_QR)
    }

    private fun showAddress(address: String) {
        binding.recipientAddressEdittext.setText(address)
    }

    //endregion
}
