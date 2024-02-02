package com.concordium.wallet.uicore.view;

/* This component class is based on an answer by @tacone on StackOverflow
 *
 * https://gist.github.com/f2face/01c8a7b089d2f74748bd12440b14dacc
 */

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

public class WrapContentHeightGridView extends GridView {

    public WrapContentHeightGridView(Context context) {
        super(context);
    }

    public WrapContentHeightGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WrapContentHeightGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightSpec = heightMeasureSpec;
        if (getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
            // The great Android "hackatlon", the love, the magic.
            // The two leftmost bits in the height measure spec have
            // a special meaning, hence we can't use them to describe height.
            heightSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthMeasureSpec, heightSpec);
    }
}
