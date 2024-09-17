package com.concordium.wallet.ui.welcome

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentImportBackupFileBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ImportBackupFileInfoBottomSheet : BottomSheetDialogFragment() {
    override fun getTheme(): Int = R.style.CCX_BottomSheetDialog

    private lateinit var binding: FragmentImportBackupFileBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImportBackupFileBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        binding.okButton.setOnClickListener { dismiss() }
    }

    private fun initViews() {
        binding.exampleFileUpIcon.post {
            applyGradientTintToIcon(
                binding.exampleFileUpIcon,
                R.color.ccx_neutral_tint_8,
                R.color.ccx_mineral_blue_tint_4,
                2.5f
            )
        }

        binding.exampleFileDownIcon.post {
            applyGradientTintToIcon(
                binding.exampleFileDownIcon,
                R.color.ccx_mineral_blue_tint_4,
                R.color.ccx_neutral_tint_8,
                1f
            )
        }

        binding.exampleFileUp.post {
            applyTextShader(
                binding.exampleFileUp,
                R.color.ccx_neutral_tint_8,
                R.color.ccx_mineral_blue_tint_4,
                2.5f
            )
        }

        binding.exampleFileDown.post {
            applyTextShader(
                binding.exampleFileDown,
                R.color.ccx_mineral_blue_tint_4,
                R.color.ccx_neutral_tint_8,
                1.5f
            )
        }
    }

    private fun applyTextShader(
        textView: TextView,
        startColor: Int,
        endColor: Int,
        widthView: Float
    ) {
        val textShader = LinearGradient(
            0f,
            0f,
            0f,
            binding.exampleFileUp.textSize * widthView,
            intArrayOf(
                requireContext().getColor(startColor),
                requireContext().getColor(endColor),
            ),
            null, Shader.TileMode.CLAMP
        )
        textView.paint.shader = textShader
        textView.alpha = 0.5f
    }

    private fun applyGradientTintToIcon(
        icon: ImageView,
        startColor: Int,
        endColor: Int,
        widthView: Float
    ) {
        val drawable = icon.drawable.mutate()
        val width = icon.width
        val height = icon.height

        if (width > 0 && height > 0) {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val gradient = LinearGradient(
                0f,
                0f,
                0f,
                height.toFloat() * widthView,
                intArrayOf(
                    requireContext().getColor(startColor),
                    requireContext().getColor(endColor),
                ), null, Shader.TileMode.CLAMP
            )
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)

            val paint = Paint().apply {
                shader = gradient
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            icon.setImageBitmap(bitmap)
            icon.alpha = 0.5f
        }
    }

    companion object {
        const val TAG = "import-backup-file"
    }
}