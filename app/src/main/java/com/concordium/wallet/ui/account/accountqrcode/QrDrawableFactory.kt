package com.concordium.wallet.ui.account.accountqrcode

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.TintAwareDrawable
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

class QrDrawableFactory {
    private val writer = QRCodeWriter()

    suspend fun getDrawable(
        content: String,
        @ColorInt
        squareColor: Int = Color.BLACK,
        errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.M,
        margin: Int = 0,
    ) = writer.encode(
        content,
        BarcodeFormat.QR_CODE,
        // 1x1 matrix yields the minimum size matrix for the data size.
        1, 1,
        mapOf(
            EncodeHintType.ERROR_CORRECTION to errorCorrectionLevel,
            EncodeHintType.MARGIN to margin,
        )
    ).let { bitMatrix ->
        checkNotNull(bitMatrix)

        BitMatrixDrawable(
            bitMatrix = bitMatrix,
            squareColor = squareColor,
        )
    }
}
