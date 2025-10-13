package com.concordium.wallet.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.Base64
import androidx.core.content.ContextCompat
import com.concordium.wallet.R

object ImageUtil {

    fun getImageBitmap(image: String): Bitmap {
        val imageBytes = Base64.decode(image, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(
            imageBytes, 0, imageBytes.size
        )
    }

    fun getIconById(context: Context, id: Int): Drawable? {
        return when (id) {
            1 -> ContextCompat.getDrawable(context, R.drawable.mw24_ic_account_profile_1)
            2 -> ContextCompat.getDrawable(context, R.drawable.mw24_ic_account_profile_2)
            3 -> ContextCompat.getDrawable(context, R.drawable.mw24_ic_account_profile_3)
            4 -> ContextCompat.getDrawable(context, R.drawable.mw24_ic_account_profile_4)
            5 -> ContextCompat.getDrawable(context, R.drawable.mw24_ic_account_profile_5)
            6 -> ContextCompat.getDrawable(context, R.drawable.mw24_ic_account_profile_6)
            7 -> ContextCompat.getDrawable(context, R.drawable.mw24_ic_account_profile_7)
            8 -> ContextCompat.getDrawable(context, R.drawable.mw24_ic_account_profile_8)
            9 -> ContextCompat.getDrawable(context, R.drawable.mw24_ic_account_profile_9)
            else -> ContextCompat.getDrawable(context, R.drawable.mw24_ic_account_profile_1)
        }
    }
}
