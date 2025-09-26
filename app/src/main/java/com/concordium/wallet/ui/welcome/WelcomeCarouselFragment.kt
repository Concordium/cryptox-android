package com.concordium.wallet.ui.welcome

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentWelcomeCarouselBinding

class WelcomeCarouselFragment : Fragment(R.layout.fragment_welcome_carousel) {

    private val binding: FragmentWelcomeCarouselBinding by lazy {
        FragmentWelcomeCarouselBinding.bind(requireView())
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}
