// This is suppress the Camera deprecation warnings.
// Update to use of Camera2 could be considered, but event the ML Kit barcode demo still uses
// the old Camera.
// An update will also require major changes in CameraSource and CameraSourcePreviews.
@file:Suppress("DEPRECATION")

package com.concordium.wallet.ui.recipient.scanqr

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.App
import com.concordium.wallet.Constants
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityScanQrBinding
import com.concordium.wallet.ui.RequestCodes
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.qr.*
import com.concordium.wallet.uicore.dialog.Dialogs
import com.concordium.wallet.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ScanQRActivity : BaseActivity(R.layout.activity_scan_qr, R.string.scan_qr_title),
    Dialogs.DialogFragmentListener {

    companion object {
        // intent request code to handle updating play services if needed.
        private const val RC_HANDLE_GMS = 9001
        private const val INVALID_QR_STATE_TIMER = 500L
        private const val EXTRA_BARCODE = "EXTRA_BARCODE"
        const val CURRENT_ACTION = "CURRENT_ACTION"
    }

    private lateinit var viewModel: ScanQRViewModel
    private val binding by lazy {
        ActivityScanQrBinding.bind(findViewById(R.id.root_layout))
    }

    private lateinit var codeScanner: CodeScanner

    private var latestInvalidBarcodeShownTime: Long = 0
    private var latestObservedInvalidBarcode: Long = 0
    private var hasFoundValidBarcode = false

    private val invalidQRStateHandler = Handler()

    private var isQrConnect = false
    private var isAddContact = false

    /**
     * This runnable has the responsibility to deactivate the invalid QR state.
     * And to not do it if new invalid detections has been found after the first.
     * Thereby avoiding switching back and forth between states to fast.
     * (But the detections are not always received from the BarcodeDetector and therefore switches in states will happen).
     */
    private val invalidQRStateRunnable = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            if (now - latestInvalidBarcodeShownTime > 2000) {
                viewModel.setStateDefaultIfAllowed()
            }

            invalidQRStateHandler.postDelayed(this, INVALID_QR_STATE_TIMER)
        }
    }

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViewModel()
        viewModel.initialize()
        initializeViews()

        hideActionBarBack(isVisible = true)

        isQrConnect = intent?.getBooleanExtra(Constants.Extras.EXTRA_QR_CONNECT, false) ?: false
        isAddContact = intent?.getBooleanExtra(Constants.Extras.EXTRA_ADD_CONTACT, false) ?: false

        invalidQRStateHandler.postDelayed(
            invalidQRStateRunnable,
            INVALID_QR_STATE_TIMER
        )

        if (!hasCameraPermission()) {
            requestCameraPermission()
        }
        setupCodeScanner()
    }

    private fun setupCodeScanner() {
        val scannerView = findViewById<CodeScannerView>(R.id.scanner_view)
        codeScanner = CodeScanner(applicationContext, scannerView)
        with(codeScanner) {
            camera = CodeScanner.CAMERA_BACK
            formats = CodeScanner.ALL_FORMATS
            autoFocusMode = AutoFocusMode.SAFE
            scanMode = ScanMode.SINGLE
            isAutoFocusEnabled = true
            isFlashEnabled = false

            decodeCallback = DecodeCallback {
                GlobalScope.launch(Dispatchers.Main) {
                    scannerView.frameColor = Color.GREEN
                    Handler().postDelayed({
                        barcodeFound(it.text)
                    }, 500)
                }
            }
            errorCallback = ErrorCallback {
                Log.e("CodeScanner#ErrorCallback: ${it.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        super.onPause()
        codeScanner.releaseResources()
    }

    override fun onDestroy() {
        super.onDestroy()

        invalidQRStateHandler.removeCallbacks(invalidQRStateRunnable)
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == RequestCodes.REQUEST_CODE_CAMERA_PERMISSION_DIALOG) {
            if (resultCode == Dialogs.POSITIVE) {
                onCameraPermissionRationaleDialogOkClicked()
            }
        }
    }

    //endregion

    //region Initialize
    // ************************************************************

    private fun initViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[ScanQRViewModel::class.java]

        viewModel.stateLiveData.observe(this) { state ->
            onStateChanged(state)
        }
    }

    private fun initializeViews() {
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun onStateChanged(state: ScanQRViewModel.State) {
        when (state) {
            ScanQRViewModel.State.DEFAULT -> showDefaultUI()
            ScanQRViewModel.State.NOT_VALID_QR -> showNotValidQRUI()
        }
    }

    private fun showDefaultUI() {
//        overlay_imageview.setImageResource(R.drawable.ic_qr_overlay)
    }

    private fun showNotValidQRUI() {
//        overlay_imageview.setImageResource(R.drawable.ic_qr_overlay_bad)
    }

    private fun goBackWithBarcode(barcode: String) {
        Log.i("goBackWithBarcode: $barcode")
        val intent = Intent()
        intent.putExtra("add_contact", isAddContact)
        intent.putExtra(EXTRA_BARCODE, barcode)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    //endregion

    //region Permission
    // ************************************************************

    private fun hasCameraPermission(): Boolean {
        val permission =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val hasCameraPermission = permission == PackageManager.PERMISSION_GRANTED
        Log.i("hasCameraPermission: $hasCameraPermission")
        return hasCameraPermission
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a message of why the permission is needed then
     * sending the request.
     */
    private fun requestCameraPermission() {
        Log.w("Camera permission is not granted. Requesting permission")
        // The rationale will be shown if the user rejects the permission a second time
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.CAMERA
            )
        ) {
            dialogs.showOkDialog(
                (this as AppCompatActivity),
                RequestCodes.REQUEST_CODE_CAMERA_PERMISSION_DIALOG,
                getString(R.string.scan_qr_permission_camera_rationale_title),
                getString(R.string.scan_qr_permission_camera_rationale)
            )
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA), RequestCodes.REQUEST_PERMISSION_CAMERA
            )
        }
    }

    private fun onCameraPermissionRationaleDialogOkClicked() {
        Log.i("onCameraPermissionRationaleDialogOkClicked")
        requestPermissions(
            arrayOf(Manifest.permission.CAMERA),
            RequestCodes.REQUEST_PERMISSION_CAMERA
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i("onRequestPermissionsResult")
        if (requestCode == RequestCodes.REQUEST_PERMISSION_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("Camera permission granted - initialize the camera source")
                // We have permission, so create the camerasource
//                createCameraSource(autoFocus, useFlash)
                // Start CameraSource here (it was not ready in onResume)
//                startCameraSource()
                return
            }
            Log.i(
                "Permission not granted: results len = " + grantResults.size +
                        " Result code = " + if (grantResults.isNotEmpty()) grantResults[0] else "(empty)"
            )
            dialogs.showOkDialog(
                this, RequestCodes.REQUEST_CODE_CAMERA_PERMISSION_NOT_AVAILABLE_DIALOG,
                R.string.scan_qr_error_no_camera_permission_title,
                R.string.scan_qr_error_no_camera_permission
            )
        }
    }

    /**
     * This is not called on the Main thread
     * @param barcode
     * @return
     */
    private fun barcodeFound(barcode: String): Boolean {
        if (hasFoundValidBarcode) {
            // ???
            Log.e("barcodeFound --> hasFoundValidBarcode: $hasFoundValidBarcode --> returning")
            return false
        }
        Log.i("barcodeFound --> isQrConnect: $isQrConnect\nbarcode.displayValue: $barcode")
        hasFoundValidBarcode = if (isQrConnect) {
            true
        } else {
            checkQrCode(barcode)
        }

        Log.i("barcodeFound --> hasFoundValidBarcode: $hasFoundValidBarcode")

        if (hasFoundValidBarcode) {
            val runnable = Runnable {
                Log.i("Valid barCode found")
                codeScanner.releaseResources()
                goBackWithBarcode(barcode)
            }
            runOnUiThread(runnable)
            return true
        } else { // Avoid spamming by checking time
            val now = System.currentTimeMillis()
            latestObservedInvalidBarcode = now
            if (now - latestInvalidBarcodeShownTime > 2000) {
                Log.e("Scanned code is not valid: $barcode")
                val runnable = Runnable {
                    viewModel.setStateQRNotValid()
                    popup.showSnackbar(binding.rootLayout, R.string.scan_qr_error_invalid_qr_code)
                }
                runOnUiThread(runnable)
                latestInvalidBarcodeShownTime = now
            }
        }
        return false
    }

    private fun checkQrCode(qrInfo: String): Boolean {
        return App.appCore.cryptoLibrary.checkAccountAddress(qrInfo)
    }

    //endregion
}
