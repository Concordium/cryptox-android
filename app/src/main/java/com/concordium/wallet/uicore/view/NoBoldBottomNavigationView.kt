package com.concordium.wallet.uicore.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.concordium.wallet.R
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarItemView
import com.google.android.material.navigation.NavigationBarMenuView

/**
 * [BottomNavigationView] with a workaround for bold selected item typeface.
 *
 * [Issue #3293](https://github.com/material-components/material-components-android/issues/3293)
 */
class NoBoldBottomNavigationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : BottomNavigationView(context, attrs) {

    private val gradientStart = ContextCompat.getColor(context, R.color.mw24_gradient_start_color)
    private val gradientCenter = ContextCompat.getColor(context, R.color.mw24_gradient_center_color)
    private val gradientEnd = ContextCompat.getColor(context, R.color.mw24_gradient_end_color)

    @SuppressLint("RestrictedApi")
    override fun createNavigationBarMenuView(context: Context): NavigationBarMenuView {
        return object : BottomNavigationMenuView(context) {
            override fun createNavigationBarItemView(context: Context): NavigationBarItemView {
                return object : BottomNavigationItemView(context) {
                    override fun setTextAppearanceActive(activeTextAppearance: Int) {
                        val largeLabel: TextView? =
                            findViewById(com.google.android.material.R.id.navigation_bar_item_large_label_view)
                        val defaultLargeLabelTypeface = largeLabel?.typeface

                        super.setTextAppearanceActive(activeTextAppearance)

                        if (defaultLargeLabelTypeface != null) {
                            largeLabel.typeface = defaultLargeLabelTypeface
                        }

                        largeLabel?.post {
                            val width = largeLabel.width.toFloat()
                            val height = largeLabel.height.toFloat()
                            if (width > 0 && height > 0) {
                                val shader = RadialGradient(
                                    0f,
                                    0f,
                                    width / 1f,
                                    intArrayOf(gradientStart, gradientCenter, gradientEnd),
                                    null,
                                    Shader.TileMode.CLAMP
                                )
                                largeLabel.paint.shader = shader
                                largeLabel.invalidate()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun setItemTextAppearanceInactive(textAppearanceRes: Int) {
        val largeLabel: TextView? =
            findViewById(com.google.android.material.R.id.navigation_bar_item_large_label_view)
        val defaultLargeLabelTypeface = largeLabel?.typeface

        super.setItemTextAppearanceInactive(textAppearanceRes)

        if (defaultLargeLabelTypeface != null) {
            largeLabel.typeface = defaultLargeLabelTypeface
        }
        largeLabel?.paint?.shader = null
        largeLabel?.invalidate()
    }
}
