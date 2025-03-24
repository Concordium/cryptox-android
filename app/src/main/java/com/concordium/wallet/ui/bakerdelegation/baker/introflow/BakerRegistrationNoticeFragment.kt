package com.concordium.wallet.ui.bakerdelegation.baker.introflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.concordium.wallet.databinding.FragmentBakerRegistrationNoticeBinding

class BakerRegistrationNoticeFragment : Fragment() {

    private lateinit var binding: FragmentBakerRegistrationNoticeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =  FragmentBakerRegistrationNoticeBinding.inflate(inflater, container, false)
        return binding.root
    }
}