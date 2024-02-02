package com.concordium.wallet.ui.passphrase.common

import android.content.Context
import android.widget.BaseAdapter
import androidx.core.content.ContextCompat
import com.concordium.wallet.R
import kotlin.math.roundToInt

abstract class WordsPickedBaseListAdapter(
    context: Context,
    private val arrayList: Array<String?>
) : BaseAdapter() {
    companion object {
        /*
            An offset made of blank invisible items is used
            to align the top and the bottom words to the vertical center.
         */
        const val BLANK = "BLANK_ITEM"
        const val OFFSET = 2
    }

    protected val defaultWordBottomPadding: Int =
        (8 /* dp */ * context.resources.displayMetrics.density).roundToInt()
    protected val currentWordBottomPadding: Int =
        (12 /* dp */ * context.resources.displayMetrics.density).roundToInt()
    protected val defaultWordTopPadding: Int = 0
    protected val currentWordTopPadding: Int = currentWordBottomPadding - defaultWordBottomPadding
    protected val defaultWordHorizontalPadding: Int =
        (9 /* dp */ * context.resources.displayMetrics.density).roundToInt()
    protected val currentWordHorizontalPadding: Int = 0
    protected val farWordHorizontalPadding: Int =
        (14 /* dp */ * context.resources.displayMetrics.density).roundToInt()
    protected val defaultWordTextColor: Int =
        ContextCompat.getColor(context, R.color.ccx_neutral_tint_4)
    protected val currentWordTextColor: Int =
        ContextCompat.getColor(context, R.color.ccx_neutral_tint_2)

    protected val filledWordBackground =
        ContextCompat.getDrawable(
            context,
            R.drawable.ccx_seed_phrase_input_word_background
        )
    protected val currentWordBackground =
        ContextCompat.getDrawable(
            context,
            R.drawable.ccx_seed_phrase_input_word_current_background
        )
    protected val emptyWordBackground =
        ContextCompat.getDrawable(
            context,
            R.drawable.ccx_seed_phrase_input_word_background
        )

    protected var wordPickedClickListener: WordPickedClickListener? = null
    var currentPosition = OFFSET

    fun interface WordPickedClickListener {
        fun onClick(position: Int)
    }

    override fun getCount(): Int {
        return arrayList.size
    }

    override fun getItem(position: Int): Any {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @JvmName("setWordPickedClickListener1")
    fun setWordPickedClickListener(wordPickedClickListener: WordPickedClickListener) {
        this.wordPickedClickListener = wordPickedClickListener
    }
}
