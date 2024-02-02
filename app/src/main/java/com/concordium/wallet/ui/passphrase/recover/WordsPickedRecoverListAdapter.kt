package com.concordium.wallet.ui.passphrase.recover

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import com.concordium.wallet.databinding.ListItemWordsPickedRecoverBinding
import com.concordium.wallet.ui.passphrase.common.WordsPickedBaseListAdapter
import kotlin.math.abs

class WordsPickedRecoverListAdapter(
    private val context: Context,
    private val arrayList: Array<String?>
) : WordsPickedBaseListAdapter(context, arrayList) {
    private var onTextChangeListener: OnTextChangeListener? = null

    fun interface OnTextChangeListener {
        fun onTextChange(text: String)
    }

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val binding: ListItemWordsPickedRecoverBinding

        if (convertView == null) {
            binding = ListItemWordsPickedRecoverBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
            holder = ViewHolder(binding)
            holder.binding.root.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }

        holder.binding.tvPosition.text = (position + 1 - OFFSET).toString()
        holder.binding.root.visibility = View.VISIBLE

        val wordContent = arrayList[position]

        if (wordContent == BLANK) {
            holder.binding.root.visibility = View.GONE
        } else if (wordContent != null) {
            holder.binding.tvTitle.text = wordContent
            holder.binding.etTitle.setText(wordContent)
        } else {
            holder.binding.tvTitle.text = ""
            holder.binding.etTitle.setText("")
        }

        val horizontalPadding =
            when (abs(position - currentPosition)) {
                0 -> currentWordHorizontalPadding
                else -> defaultWordHorizontalPadding
                // TODO: Padding becomes inconsistent when using 3 variants.
                //else -> farWordHorizontalPadding
            }
        holder.binding.root.setPaddingRelative(
            horizontalPadding,

            if (position == currentPosition)
                currentWordTopPadding
            else
                defaultWordTopPadding,

            horizontalPadding,

            if (position == currentPosition)
                currentWordBottomPadding
            else
                defaultWordBottomPadding
        )

        when {
            position == currentPosition -> {
                holder.binding.containerLayout.background = currentWordBackground
                holder.binding.tvTitle.setTextColor(currentWordTextColor)
                holder.binding.etTitle.setTextColor(currentWordTextColor)
            }

            holder.binding.tvTitle.text.isNotEmpty() -> {
                holder.binding.containerLayout.background = filledWordBackground
                holder.binding.tvTitle.setTextColor(defaultWordTextColor)
                holder.binding.etTitle.setTextColor(defaultWordTextColor)
            }

            else -> {
                holder.binding.containerLayout.background = emptyWordBackground
                holder.binding.tvTitle.setTextColor(defaultWordTextColor)
                holder.binding.etTitle.setTextColor(defaultWordTextColor)
            }
        }

        holder.binding.rlInput.setOnClickListener {
            wordPickedClickListener?.onClick(position)
        }

        holder.binding.etTitle.doOnTextChanged { text, _, _, _ ->
            onTextChangeListener?.onTextChange(text.toString())
        }

        holder.binding.etTitle.showSoftInputOnFocus = true

        if (position == currentPosition) {
            holder.binding.etTitle.visibility = View.VISIBLE
            holder.binding.tvTitle.visibility = View.GONE
            holder.binding.etTitle.requestFocus()
            holder.binding.etTitle.setSelection(holder.binding.etTitle.text.length)
        } else {
            holder.binding.etTitle.visibility = View.GONE
            holder.binding.tvTitle.visibility = View.VISIBLE
        }

        return holder.binding.root
    }

    fun setOnTextChangeListener(onTextChangeListener: OnTextChangeListener) {
        this.onTextChangeListener = onTextChangeListener
    }

    private inner class ViewHolder(val binding: ListItemWordsPickedRecoverBinding)
}
