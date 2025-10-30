package com.concordium.wallet.ui.account.accountqrcode

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.FragmentReceiveBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.uicore.toast.showCustomToast
import com.concordium.wallet.util.ClipboardUtil
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ReceiveFragment : Fragment() {

    private lateinit var binding: FragmentReceiveBinding

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity())[MainViewModel::class.java]
    }
    private val viewModel: ReceiveViewModel by viewModel {
        parametersOf(mainViewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReceiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        viewModel.activeAccount.collectWhenStarted(viewLifecycleOwner) {
            binding.shareLayout.isEnabled = it != null
            binding.copyAddressLayout.isEnabled = it != null
            it?.let(::updateViews)
        }
    }

    private fun initViews() {
        binding.shareLayout.setOnClickListener {
            shareAddress()
        }
        binding.copyAddressLayout.setOnClickListener {
            copyAddress()
        }
    }

    private fun updateViews(account: Account) {
        val qrImage = generateQR(account.address)
        if (qrImage != null) {
            binding.addressQrImageview.setImageBitmap(qrImage)
        }
        binding.accountTitleTextview.text = account.getAccountName()
        binding.addressQrTextview.text = account.address
    }

    private fun shareAddress() {
        val sendIntent = Intent(Intent.ACTION_SEND)
        sendIntent.type = "text/plain"
        sendIntent.putExtra(Intent.EXTRA_TEXT, viewModel.getAccount()?.address)
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun copyAddress() {
        ClipboardUtil.copyToClipboard(
            context = requireContext(),
            text = viewModel.getAccount()?.address ?: ""
        )

        requireActivity().showCustomToast(title = getString(R.string.account_qr_code_copied))
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
                    pixels[offset + x] = if (bitMatrix[x, y]) Color.WHITE else Color.TRANSPARENT
                }
            }

            val bitmap = createBitmap(width, height)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            return null
        }
    }
}
