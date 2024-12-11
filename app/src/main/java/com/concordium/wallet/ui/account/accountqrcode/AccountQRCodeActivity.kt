package com.concordium.wallet.ui.account.accountqrcode

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityAccountQrCodeBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.GradientAnimator
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

class AccountQRCodeActivity :
    BaseActivity(R.layout.activity_account_qr_code, R.string.account_qr_code_title) {

    private lateinit var viewModel: AccountQRCodeViewModel
    private val binding by lazy {
        ActivityAccountQrCodeBinding.bind(findViewById(R.id.root_layout))
    }

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
    }

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val account = intent.extras!!.getSerializable(EXTRA_ACCOUNT) as Account
        initializeViewModel()
        viewModel.initialize(account)
        initViews()
        hideActionBarBack(isVisible = true)
    }

    //endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(AccountQRCodeViewModel::class.java)
    }

    private fun initViews() {
        val qrImage = generateQR(viewModel.account.address)
        if (qrImage != null) {
            binding.addressQrImageview.setImageBitmap(qrImage)
        }
        GradientAnimator().applyGradientAnimation(this, binding.addressLayout)
        binding.accountTitleTextview.text =
            getString(R.string.account_qr_code_to_account, viewModel.account.getAccountName())
        binding.addressQrTextview.text = viewModel.account.address
        binding.shareLayout.setOnClickListener {
            shareAddress()
        }
        binding.copyAddressLayout.setOnClickListener {
            copyAddress()
        }
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun shareAddress() {
        val sendIntent = Intent(Intent.ACTION_SEND)
        sendIntent.type = "text/plain"
        sendIntent.putExtra(Intent.EXTRA_TEXT, viewModel.account.address)
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun copyAddress() {
        val clipboardManager: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("txt", viewModel.account.address)
        clipboardManager.setPrimaryClip(clipData)

        binding.copiedMessageTextView.isVisible = true
    }

    private fun generateQR(qrCodeContent: String): Bitmap? {
        try {
            val writer = QRCodeWriter()
            val bitMatrix: BitMatrix =
                writer.encode(qrCodeContent, BarcodeFormat.QR_CODE, 256, 256)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) Color.BLACK else Color.TRANSPARENT
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            return null
        }
    }

    //endregion
}
