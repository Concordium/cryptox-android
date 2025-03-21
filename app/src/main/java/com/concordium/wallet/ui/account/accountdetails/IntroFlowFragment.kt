package com.concordium.wallet.ui.account.accountdetails

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.concordium.wallet.databinding.FragmentIntroFlowBinding
import com.concordium.wallet.uicore.handleUrlClicks

class IntroFlowFragment : Fragment() {
    private var _binding: FragmentIntroFlowBinding? = null
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
        _binding = FragmentIntroFlowBinding.inflate(inflater, container, false)

        val linkText = requireContext().assets.open(link).bufferedReader().use { it.readText() }
        binding.title.text = Html.fromHtml(resources.getString(title), Html.FROM_HTML_MODE_LEGACY)
        binding.description.text = Html.fromHtml(linkText, Html.FROM_HTML_MODE_LEGACY)
        binding.description.handleUrlClicks { url ->
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            ContextCompat.startActivity(requireContext(), browserIntent, null)
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val LINK_EXTRA = "link"
        private const val TITLE_EXTRA = "title"

        fun newInstance(link: String, @StringRes title: Int) = IntroFlowFragment().apply {
            arguments = Bundle().apply {
                putString(LINK_EXTRA, link)
                putInt(TITLE_EXTRA, title)
            }
        }
    }
}
