package com.concordium.wallet.uicore.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import com.concordium.wallet.R
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarItemView
import com.google.android.material.navigation.NavigationBarMenuView

class MW24BottomNavigationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
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
                        super.setTextAppearanceActive(activeTextAppearance)

                        val largeLabel: TextView? =
                            findViewById(com.google.android.material.R.id.navigation_bar_item_large_label_view)

                        largeLabel?.doOnPreDraw {
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
        super.setItemTextAppearanceInactive(textAppearanceRes)

        val largeLabel: TextView? =
            findViewById(com.google.android.material.R.id.navigation_bar_item_large_label_view)

        largeLabel?.paint?.shader = null
        largeLabel?.invalidate()
    }
}
