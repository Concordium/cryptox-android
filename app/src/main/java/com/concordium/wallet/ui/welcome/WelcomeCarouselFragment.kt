package com.concordium.wallet.ui.welcome

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.viewpager2.widget.ViewPager2
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentWelcomeCarouselBinding

class WelcomeCarouselFragment : Fragment(R.layout.fragment_welcome_carousel) {

    private val binding: FragmentWelcomeCarouselBinding by lazy {
        FragmentWelcomeCarouselBinding.bind(requireView())
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = WelcomeCarouselAdapter()
        binding.viewPager.adapter = adapter

        binding.dotsIndicator.attachTo(binding.viewPager)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.continueButton.text =
                    if (position == adapter.itemCount - 1)
                        getString(R.string.welcome_get_started)
                    else
                        getString(R.string.welcome_continue)
            }
        })

        binding.continueButton.setOnClickListener {
            if (binding.viewPager.currentItem == adapter.itemCount - 1) {
                setFragmentResult(
                    GET_STARTED_REQUEST,
                    Bundle()
                )
            } else {
                binding.viewPager.setCurrentItem(
                    binding.viewPager.currentItem + 1,
                    true
                )
            }
        }
    }

    companion object {
        const val GET_STARTED_REQUEST = "get-started-request"
    }
}
