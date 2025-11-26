package com.concordium.wallet.uicore.toast

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import com.concordium.wallet.R
import com.concordium.wallet.databinding.CustomToastBinding

fun Context.showCustomToast(
    title: String,
    toastType: ToastType = ToastType.SUCCESS,
) {
    val binding = CustomToastBinding.inflate(LayoutInflater.from(this))
    val context = binding.root.context

    binding.toastTitle.text = title

    when (toastType) {
        ToastType.SUCCESS -> {
            binding.toastLayout.background = AppCompatResources.getDrawable(
                context,
                R.drawable.mw24_toast_background_success
            )
            binding.toastTitle.setTextColor(context.getColor(R.color.mw24_content_success_primary))
            binding.toastImage.setImageResource(R.drawable.mw24_ic_circled_check_done)
            binding.toastImage.imageTintList =
                AppCompatResources.getColorStateList(
                    context,
                    R.color.mw24_content_success_primary
                )
            binding.toastCloseImage.imageTintList =
                AppCompatResources.getColorStateList(
                    context,
                    R.color.mw24_content_success_secondary
                )
        }

        ToastType.INFO -> {
            binding.toastLayout.background = AppCompatResources.getDrawable(
                context,
                R.drawable.mw24_toast_background_info
            )
            binding.toastTitle.setTextColor(context.getColor(R.color.mw24_content_inverse_primary))
            binding.toastImage.setImageResource(R.drawable.mw24_ic_tooltip_info)
            binding.toastImage.imageTintList =
                AppCompatResources.getColorStateList(
                    context,
                    R.color.mw24_content_inverse_primary
                )
            binding.toastCloseImage.imageTintList =
                AppCompatResources.getColorStateList(
                    context,
                    R.color.mw24_content_secondary
                )
        }

        ToastType.ERROR -> {
            binding.toastLayout.background = AppCompatResources.getDrawable(
                context,
                R.drawable.mw24_toast_background_error
            )
            binding.toastTitle.setTextColor(context.getColor(R.color.mw24_content_error_primary))
            binding.toastImage.setImageResource(R.drawable.mw24_ic_onramp_close)
            binding.toastImage.imageTintList =
                AppCompatResources.getColorStateList(
                    context,
                    R.color.mw24_content_error_primary
                )
            binding.toastCloseImage.imageTintList =
                AppCompatResources.getColorStateList(
                    context,
                    R.color.mw24_content_error_secondary
                )
        }
    }

    val toast = Toast(this)
    toast.duration = Toast.LENGTH_SHORT
    toast.setGravity(Gravity.BOTTOM, 0, 100)
    toast.view = binding.root
    toast.show()
}
