package com.concordium.wallet.ui.account.accountdetails

import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.concordium.wallet.databinding.FragmentWebviewPageBinding

class WebViewPageFragment : Fragment() {
    private var _binding: FragmentWebviewPageBinding? = null
    private val binding get() = _binding!!

    private val link: String
        get() = requireNotNull(arguments?.getString(LINK_EXTRA)) {
            "No link specified"
        }
    private val title: Int
        @StringRes
        get() = requireNotNull(arguments?.getInt(TITLE_EXTRA, -1)?.takeIf { it > 0 }) {
            "No title specified"
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWebviewPageBinding.inflate(inflater, container, false)
        binding.webviewContent.loadUrl(link)
        binding.webviewContent.setBackgroundColor(Color.TRANSPARENT)
        binding.title.text = Html.fromHtml(resources.getString(title))
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val LINK_EXTRA = "link"
        private const val TITLE_EXTRA = "title"

        fun newInstance(link: String, @StringRes title: Int) = WebViewPageFragment().apply {
            arguments = Bundle().apply {
                putString(LINK_EXTRA, link)
                putInt(TITLE_EXTRA, title)
            }
        }
    }
}
