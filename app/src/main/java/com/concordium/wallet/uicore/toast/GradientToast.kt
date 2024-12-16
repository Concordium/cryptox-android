package com.concordium.wallet.uicore.toast

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Toast
import com.concordium.wallet.databinding.GradientToastBinding

fun Context.showGradientToast(iconResId: Int, title: String) {
    val binding = GradientToastBinding.inflate(LayoutInflater.from(this))

    binding.toastImage.setImageResource(iconResId)
    binding.toastTitle.text = title

    val toast = Toast(this)
    toast.duration = Toast.LENGTH_SHORT
    toast.setGravity(Gravity.BOTTOM, 0, 100)
    toast.view = binding.root
    toast.show()
}