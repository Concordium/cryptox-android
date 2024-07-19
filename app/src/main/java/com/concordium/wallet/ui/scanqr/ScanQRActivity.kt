package com.concordium.wallet.ui.scanqr

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
import com.concordium.wallet.databinding.ActivityScanQrBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.Intents
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ScanQRActivity : BaseActivity(R.layout.activity_scan_qr, R.string.scan_qr_title) {

    private lateinit var binding: ActivityScanQrBinding
    private var hasCameraPermission: Boolean = false
    private var isDecodingRequired: Boolean = false
    private val isQrConnect: Boolean by lazy {
        intent.getBooleanExtra(Constants.Extras.EXTRA_QR_CONNECT, false)
    }
    private val cameraPermissionRequestLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
            Manifest.permission.CAMERA,
            this::onCameraPermissionResult
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScanQrBinding.bind(findViewById(R.id.toastLayoutTopError))

        initScanner()
        cameraPermissionRequestLauncher.launch(Unit)

        hideActionBarBack(isVisible = true)
    }

    private fun initScanner() {
        isDecodingRequired = true

        binding.scannerView.initializeFromIntent(
            IntentIntegrator(this)
                .setBeepEnabled(false)
                .setDesiredBarcodeFormats(BarcodeFormat.QR_CODE.name)
                // Mixed scan enables both normal and inverted QR codes.
                .addExtra(Intents.Scan.SCAN_TYPE, Intents.Scan.MIXED_SCAN)
                .createScanIntent()
        )
        binding.scannerView.statusView.isVisible = false
    }

    private fun resumePreviewIfAllowed() {
        if (hasCameraPermission) {
            binding.scannerView.resume()
        }
    }

    private fun resumeDecodingIfRequired() {
        if (!isDecodingRequired) {
            Log.i("resumeDecodingIfRequired: not required")
            return
        }

        Log.i("resumeDecodingIfRequired: resuming")

        binding.scannerView.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                handleDecodedContent(result.text)
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
                // We don't care about points.
            }
        })
    }

    fun handleDecodedContent(content: String) {
        isDecodingRequired = false

        Log.d("handleDecodedContent: $content")

        when {
            // Whatever the connection screen wants.
            isQrConnect ->
                finishWithResult(content)

            // Valid account address.
            App.appCore.cryptoLibrary.checkAccountAddress(content) ->
                finishWithResult(content)

            else ->
                showDecodingErrorAndRetry(getString(R.string.scan_qr_error_invalid_qr_code))
        }
    }

    private fun finishWithResult(content: String) {
        Log.i("finishWithResult: $content")

        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtra(
                    Constants.Extras.EXTRA_ADD_CONTACT,
                    intent.getBooleanExtra(Constants.Extras.EXTRA_ADD_CONTACT, false)
                )
                .putExtra(Constants.Extras.EXTRA_SCANNED_QR_CONTENT, content)
        )
        finish()
    }

    private fun finishWithError(error: String) {
        Toast.makeText(
            this,
            error,
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    private var retryDecodingJob: Job? = null
    private fun showDecodingErrorAndRetry(error: String) {
        Log.i("showDecodingErrorAndRetry")

        retryDecodingJob?.cancel()

        showError(error)

        retryDecodingJob = lifecycleScope.launch {
            delay(ERROR_TIMEOUT_MS)
            isDecodingRequired = true
            resumeDecodingIfRequired()
        }
    }

    override fun onResume() {
        super.onResume()
        resumePreviewIfAllowed()
        resumeDecodingIfRequired()
    }

    override fun onPause() {
        super.onPause()
        binding.scannerView.pause()
    }

    private fun onCameraPermissionResult(isGranted: Boolean) {
        hasCameraPermission = isGranted
        resumePreviewIfAllowed()
        if (!isGranted) {
            finishWithError(getString(R.string.scan_qr_permission_camera_rationale))
        }
    }

    companion object {
        private const val ERROR_TIMEOUT_MS = 2000L

        fun getScannedQrContent(result: Bundle) =
            checkNotNull(result.getString(Constants.Extras.EXTRA_SCANNED_QR_CONTENT)) {
                "No scanned QR content in the result"
            }

        fun isAddContact(result: Bundle) =
            result.getBoolean(Constants.Extras.EXTRA_ADD_CONTACT, false)
    }
}
