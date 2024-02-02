package com.concordium.wallet.uicore.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
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
                    }
                }
            }
        }
    }
}
