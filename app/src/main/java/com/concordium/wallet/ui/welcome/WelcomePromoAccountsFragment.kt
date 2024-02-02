package com.concordium.wallet.ui.welcome

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.toSpannable
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.FragmentWelcomePromoAccountsBinding

class WelcomePromoAccountsFragment : Fragment() {
    private lateinit var binding: FragmentWelcomePromoAccountsBinding
    private lateinit var promoViewModel: WelcomePromoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        promoViewModel = ViewModelProvider(requireActivity()).get()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWelcomePromoAccountsBinding
            .inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activateAndGetTextSpannable =
            SpannableStringBuilder().apply {
                append("*")
                setSpan(
                    ImageSpan(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.ccx_ico_star_resized
                        )!!.apply {
                            setBounds(
                                0,
                                0,
                                intrinsicWidth,
                                intrinsicHeight,
                            )
                        }
                    ),
                    length - 1,
                    length,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                )
                append(" ")
                append(getString(R.string.welcome_promo_activate_and_get))
            }.toSpannable()
        binding.activateAndGetTextView.text = activateAndGetTextSpannable

        binding.rewardTextView.text =
            CurrencyUtil.formatGTU(promoViewModel.accountActivationReward, false)

        binding.activateButton.setOnClickListener {
            (activity as? WelcomeAccountActivationLauncher)
                ?.proceedWithAccountActivation()
        }

        binding.aiAssistantLayout.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(promoViewModel.aiAssistantUrl))
            ContextCompat.startActivity(requireContext(), browserIntent, null)
        }

        binding.watchVideoButton.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(promoViewModel.videosUrl))
            ContextCompat.startActivity(requireContext(), browserIntent, null)
        }
    }
}
