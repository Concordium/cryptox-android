package com.concordium.wallet.ui.passphrase.recover

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentPassPhraseRecoverInputBinding
import com.concordium.wallet.ui.passphrase.common.WordsPickedBaseListAdapter
import com.concordium.wallet.util.KeyboardUtil
import com.concordium.wallet.util.Log
import java.util.Timer
import kotlin.concurrent.schedule

class PassPhraseRecoverInputFragment : Fragment() {
    private var _binding: FragmentPassPhraseRecoverInputBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PassPhraseRecoverViewModel
        get() = (requireActivity() as RecoverWalletActivity).viewModel

    private lateinit var arrayAdapter: WordsPickedRecoverListAdapter
    private var snapTimer: Timer? = null
    private var listViewHasFocus = true
    private var listViewScrollState = SCROLL_STATE_IDLE
    private val suggestionTextViews: List<TextView>
        get() = listOf(
            binding.tvSuggest1,
            binding.tvSuggest2,
            binding.tvSuggest3,
            binding.tvSuggest4
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPassPhraseRecoverInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadAllWords()
        initViews()
        initObservers()
    }

    override fun onResume() {
        super.onResume()
        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
    }

    override fun onPause() {
        super.onPause()
        KeyboardUtil.hideKeyboard(requireActivity())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeSnapTimer()
        _binding = null
    }

    private fun initObservers() {
        viewModel.validate.observe(viewLifecycleOwner) { success ->
            binding.errorTextView.isVisible = !success
        }
    }

    private fun initViews() {
        initButtons()

        hideAllSuggestions()

        arrayAdapter = WordsPickedRecoverListAdapter(requireContext(), viewModel.wordsPicked)

        arrayAdapter.setWordPickedClickListener { position ->
            arrayAdapter.currentPosition = position
            moveToCurrent()
        }

        arrayAdapter.setOnTextChangeListener { text ->
            // Handle paste only in the first word input.
            val splitWords = text.splitInputAsWords()
            if (arrayAdapter.currentPosition == WordsPickedBaseListAdapter.OFFSET && splitWords.size > 1) {
                replaceWordsAndValidate(splitWords)
            } else {
                lookUp(text)
            }
        }
        arrayAdapter.also { binding.wordsListView.adapter = it }

        initListViewScroll()
    }

    private fun initButtons() {
        binding.btnClearAll.setOnClickListener {
            viewModel.clearWordsPicked()
            initViews()
            arrayAdapter.notifyDataSetChanged()
        }
        binding.btnClearBelow.setOnClickListener {
            viewModel.clearWordsBelow(arrayAdapter.currentPosition)
            hideAllSuggestions()
            arrayAdapter.notifyDataSetChanged()
        }
        binding.pasteButton.setOnClickListener {
            tryPasteFromClipboard()
        }
        binding.tvSuggest1.setOnClickListener {
            insertSuggestion(binding.tvSuggest1)
        }
        binding.tvSuggest2.setOnClickListener {
            insertSuggestion(binding.tvSuggest2)
        }
        binding.tvSuggest3.setOnClickListener {
            insertSuggestion(binding.tvSuggest3)
        }
        binding.tvSuggest4.setOnClickListener {
            insertSuggestion(binding.tvSuggest4)
        }
    }

    private fun lookUp(text: String) {
        if (listViewScrollState != SCROLL_STATE_IDLE)
            return

        hideAllSuggestions()

        if (text.length <= 1)
            return

        val filtered = viewModel.allWords
            .filter { it.startsWith(text) }
            .let { it.subList(0, it.size.coerceAtMost(4)) }

        suggestionTextViews.forEachIndexed { i, suggestionTextView ->
            val suggestion = filtered.getOrNull(i)
            suggestionTextView.text = suggestion
            suggestionTextView.isVisible = suggestion != null
        }

        binding.linesImageView.setImageDrawable(
            when (filtered.size) {
                1 ->
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.cryptox_phrase_input_lines_1
                    )

                2 ->
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.cryptox_phrase_input_lines_2
                    )

                3 ->
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.cryptox_phrase_input_lines_3
                    )

                4 ->
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.cryptox_phrase_input_lines_4
                    )

                else -> null
            }
        )

        binding.wordErrorTextView.isVisible = filtered.isEmpty()
    }

    private fun hideAllSuggestions() {
        suggestionTextViews.forEach { it.isVisible = false }
        binding.linesImageView.setImageDrawable(null)
        binding.wordErrorTextView.isVisible = false
    }

    private fun initListViewScroll() {
        binding.wordsListView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                listViewScrollState = scrollState
                if (listViewScrollState == SCROLL_STATE_TOUCH_SCROLL)
                    listViewHasFocus = true
            }

            override fun onScroll(
                view: AbsListView,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                val changeTo = firstVisibleItem + 2
                if (changeTo != arrayAdapter.currentPosition && listViewHasFocus) {
                    if (arrayAdapter.currentPosition != changeTo) {
                        arrayAdapter.currentPosition = changeTo
                        startSnapTimer()
                        arrayAdapter.notifyDataSetChanged()
                    }
                }
            }
        })
    }

    private fun removeSnapTimer() {
        snapTimer?.cancel()
        snapTimer?.purge()
        snapTimer = null
    }

    private fun startSnapTimer() {
        if (snapTimer == null) {
            snapTimer = Timer()
            snapTimer?.schedule(50) {
                activity?.runOnUiThread {
                    if (listViewScrollState == SCROLL_STATE_IDLE && listViewHasFocus) {
                        binding.wordsListView.smoothScrollToPositionFromTop(
                            arrayAdapter.currentPosition - WordsPickedBaseListAdapter.OFFSET,
                            0,
                            100
                        )
                        hideAllSuggestions()
                        removeSnapTimer()
                    } else {
                        removeSnapTimer()
                        startSnapTimer()
                    }
                }
            }
        }
    }

    private fun insertSuggestion(tvSuggestion: TextView) {
        if (tvSuggestion.visibility != View.VISIBLE)
            return

        if (arrayAdapter.currentPosition < PassPhraseRecoverViewModel.WORD_COUNT + WordsPickedBaseListAdapter.OFFSET) {
            viewModel.wordsPicked[arrayAdapter.currentPosition] = tvSuggestion.text.toString()
            hideAllSuggestions()
            moveDown()
            viewModel.validateInputCode()
        }
    }

    private fun moveDown() {
        if (arrayAdapter.currentPosition <= PassPhraseRecoverViewModel.WORD_COUNT)
            arrayAdapter.currentPosition++
        moveToCurrent()
    }

    private fun moveToCurrent() {
        listViewHasFocus = false
        binding.wordsListView.smoothScrollToPositionFromTop(
            arrayAdapter.currentPosition - WordsPickedBaseListAdapter.OFFSET,
            0,
            50
        )
        Timer().schedule(100) {
            activity?.runOnUiThread {
                hideAllSuggestions()
                arrayAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun tryPasteFromClipboard() {
        val clipboardSplitWords = requireContext().getSystemService(ClipboardManager::class.java)
            ?.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.text
            ?.toString()
            ?.trim()
            ?.splitInputAsWords()

        if (clipboardSplitWords.isNullOrEmpty()) {
            Log.d("clipboard_text_blank")
            return
        }

        replaceWordsAndValidate(
            replacement = clipboardSplitWords,
        )
    }

    private fun replaceWordsAndValidate(replacement: Collection<String>) {
        // Insert as many words as possible overwriting the current input.
        val startWordIndex = WordsPickedBaseListAdapter.OFFSET
        var lastInsertedWordIndex = -1
        replacement.forEachIndexed { replacementIndex, replacementWord ->
            val wordIndex = replacementIndex + startWordIndex
            if (wordIndex < viewModel.wordsPicked.size && viewModel.wordsPicked[wordIndex] != WordsPickedBaseListAdapter.BLANK) {
                viewModel.wordsPicked[wordIndex] = replacementWord
                lastInsertedWordIndex = wordIndex
            }
        }

        // If inserted at least something, validate the input.
        if (lastInsertedWordIndex != -1) {
            hideAllSuggestions()
            arrayAdapter.currentPosition = lastInsertedWordIndex
            moveToCurrent()
            KeyboardUtil.hideKeyboardInFragment(requireContext(), binding.wordsListView)
            viewModel.validateInputCode()
        }
    }

    private fun String.splitInputAsWords() =
        split(INPUT_WORDS_SPLIT_REGEX).filter(String::isNotBlank)

    private companion object {
        /**
         * Split the input by whitespace(s) or line break(s).
         */
        private val INPUT_WORDS_SPLIT_REGEX = "[\\s\\n]+".toRegex()
    }
}
