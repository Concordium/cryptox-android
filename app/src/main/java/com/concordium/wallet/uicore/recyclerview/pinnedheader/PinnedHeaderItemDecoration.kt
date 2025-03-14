package com.concordium.wallet.uicore.recyclerview.pinnedheader

import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class PinnedHeaderItemDecoration(private val listener: PinnedHeaderListener) :
    RecyclerView.ItemDecoration() {

    private var headerHeight: Int = 0
    private var cachedHeader: View? = null
    private var cachedHeaderPosition: Int = RecyclerView.NO_POSITION

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        val topChild = parent.getChildAt(0) ?: return
        val topChildPosition = parent.getChildAdapterPosition(topChild)
        if (topChildPosition == RecyclerView.NO_POSITION) return

        val headerPosition = listener.getHeaderPositionForItem(topChildPosition)
        val currentHeader = if (cachedHeaderPosition == headerPosition && cachedHeader != null) {
            cachedHeader!!
        } else {
            getHeaderViewForItem(headerPosition, parent)
        }

        fixLayoutSize(parent, currentHeader)
        val contactPoint = currentHeader.bottom
        val childInContact = getChildInContact(parent, contactPoint)

        if (childInContact != null && listener.isHeader(parent.getChildAdapterPosition(childInContact))) {
            moveHeader(c, currentHeader, childInContact)
        } else {
            drawHeader(c, currentHeader)
        }
    }

    private fun getHeaderViewForItem(itemPosition: Int, parent: RecyclerView): View {
        val headerPosition = listener.getHeaderPositionForItem(itemPosition)

        if (cachedHeaderPosition != headerPosition || cachedHeader == null) {
            val layoutResId = listener.getHeaderLayout(headerPosition)
            val header = LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
            listener.bindHeaderData(header, headerPosition)
            cachedHeader = header
            cachedHeaderPosition = headerPosition
        }
        return cachedHeader!!
    }

    private fun drawHeader(c: Canvas, header: View) {
        c.save()
        c.translate(0f, 0f)
        header.draw(c)
        c.restore()
    }

    private fun moveHeader(c: Canvas, currentHeader: View, nextHeader: View) {
        c.save()
        c.translate(0f, (nextHeader.top - currentHeader.height).toFloat())
        currentHeader.draw(c)
        c.restore()
    }

    private fun getChildInContact(parent: RecyclerView, contactPoint: Int): View? {
        return (0 until parent.childCount)
            .map { parent.getChildAt(it) }
            .firstOrNull { it.bottom > contactPoint && it.top <= contactPoint }
    }

    /**
     * Properly measures and layouts the top sticky header.
     * @param parent ViewGroup: RecyclerView in this case.
     */
    private fun fixLayoutSize(parent: ViewGroup, view: View) {

        // Specs for parent (RecyclerView)
        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val heightSpec =
            View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)

        // Specs for children (headers)
        val childWidthSpec =
            ViewGroup.getChildMeasureSpec(
                widthSpec,
                parent.paddingLeft + parent.paddingRight,
                view.layoutParams.width
            )
        val childHeightSpec = ViewGroup.getChildMeasureSpec(
            heightSpec,
            parent.paddingTop + parent.paddingBottom,
            view.layoutParams.height
        )

        view.measure(childWidthSpec, childHeightSpec)

        headerHeight = view.measuredHeight
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }
}