package com.concordium.wallet.ui.recipient.scanqr

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.concordium.wallet.App
import com.concordium.wallet.Constants
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityNewScanQrBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NewScanQRActivity : BaseActivity(R.layout.activity_new_scan_qr, R.string.scan_qr_title) {

    private lateinit var binding: ActivityNewScanQrBinding
    private var hasCameraPermission: Boolean = false
    private var qrScanIsRequired: Boolean = false
    private val isQrConnect: Boolean by lazy {
        intent.getBooleanExtra(Constants.Extras.EXTRA_QR_CONNECT, false)
    }
    private val isAddContact: Boolean by lazy {
        intent.getBooleanExtra(Constants.Extras.EXTRA_ADD_CONTACT, false)
    }
    private val cameraPermissionRequestLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
            Manifest.permission.CAMERA,
            this::onCameraPermissionResult
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNewScanQrBinding.bind(findViewById(R.id.toastLayoutTopError))

        initQrScanner()
        cameraPermissionRequestLauncher.launch(Unit)

        hideActionBarBack(isVisible = true)
    }

    private fun initQrScanner() {
        qrScanIsRequired = true
        binding.scannerView.initializeFromIntent(
            IntentIntegrator(this)
                .setBeepEnabled(false)
                .setDesiredBarcodeFormats(listOf(BarcodeFormat.QR_CODE.name))
                .createScanIntent()
        )
        binding.scannerView.statusView.isVisible = false
    }

    private fun resumeQrPreviewIfAllowed() {
        if (hasCameraPermission) {
            binding.scannerView.resume()
        }
    }

    private fun resumeQrScanIfRequired() {
        if (!qrScanIsRequired) {
            Log.i("resumeQrScanIfRequired: not required")
            return
        }

        Log.i("resumeQrScanIfRequired: resuming")

        binding.scannerView.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                handleQrCodeContent(result.text)
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
                // We don't care about points.
            }
        })
    }

    fun handleQrCodeContent(content: String) {
        qrScanIsRequired = false

        lifecycleScope.launch {
            Log.d("handleQrCodeContent: $content")

            when {
                // Whatever the connection screen wants.
                isQrConnect ->
                    goBackWithQrCodeContent(content)

                // Valid account address.
                App.appCore.cryptoLibrary.checkAccountAddress(content) ->
                    goBackWithQrCodeContent(content)

                else ->
                    showQrScanErrorAndRetry(getString(R.string.scan_qr_error_invalid_qr_code))
            }
        }
    }

    private fun goBackWithQrCodeContent(content: String) {
        Log.i("goBackWithQrCodeContent: $content")

        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtra(Constants.Extras.EXTRA_ADD_CONTACT, isAddContact)
                .putExtra(Constants.Extras.EXTRA_SCANNED_QR_CONTENT, content)
        )
        finish()
    }

    private fun goBackWithError(error: String) {
        Toast.makeText(
            this,
            error,
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    private var retryQrScanJob: Job? = null
    private fun showQrScanErrorAndRetry(error: String) {
        Log.i("showQrScanErrorAndRetry")

        retryQrScanJob?.cancel()

        showError(error)

        retryQrScanJob = lifecycleScope.launch {
            delay(2000)

            qrScanIsRequired = true
            resumeQrScanIfRequired()
        }
    }

    override fun onResume() {
        super.onResume()
        resumeQrPreviewIfAllowed()
        resumeQrScanIfRequired()
    }

    override fun onPause() {
        super.onPause()
        binding.scannerView.pause()
    }

    private fun onCameraPermissionResult(isGranted: Boolean) {
        hasCameraPermission = true
        if (isGranted) {
            resumeQrPreviewIfAllowed()
        } else {
            goBackWithError(getString(R.string.scan_qr_permission_camera_rationale))
        }
    }

    companion object {
        fun getScannedQrContent(result: Bundle) =
            checkNotNull(result.getString(Constants.Extras.EXTRA_SCANNED_QR_CONTENT)) {
                "No scanned QR content in the result"
            }

        fun isAddContact(result: Bundle) =
            result.getBoolean(Constants.Extras.EXTRA_ADD_CONTACT, false)
    }
}
