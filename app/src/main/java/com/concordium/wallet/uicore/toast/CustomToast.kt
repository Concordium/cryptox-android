package com.concordium.wallet.uicore.toast

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Toast
import com.concordium.wallet.R
import com.concordium.wallet.databinding.CustomToastBinding

fun Context.showCustomToast(
    iconResId: Int = R.drawable.mw24_ic_circled_check_done,
    title: String
) {
    val binding = CustomToastBinding.inflate(LayoutInflater.from(this))

    binding.toastImage.setImageResource(iconResId)
    binding.toastTitle.text = title

    val toast = Toast(this)
    toast.duration = Toast.LENGTH_SHORT
    toast.setGravity(Gravity.BOTTOM, 0, 100)
    toast.view = binding.root
    toast.show()
}
