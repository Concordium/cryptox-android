package com.concordium.wallet.uicore.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView

/**
 * A [NestedScrollView] that first scrolls itself and then the [RecyclerView] child.
 * Useful for collapsing header without coordinator layout
 */
class ScrollingHeaderNestedScrollView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : NestedScrollView(context, attrs, defStyleAttr) {

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (target is RecyclerView) {
            if (dy > 0 && canScrollVertically(1)) {
                consumed[1] = dy
                scrollBy(0, dy)
            } else {
                super.onNestedPreScroll(target, dx, dy, consumed, type)
            }
        } else {
            super.onNestedPreScroll(target, dx, dy, consumed, type)
        }
    }
}
