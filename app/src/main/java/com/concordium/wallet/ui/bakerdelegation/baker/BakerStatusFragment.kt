package com.concordium.wallet.ui.bakerdelegation.baker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.concordium.wallet.databinding.FragmentBakerStatusBinding

class BakerStatusFragment : Fragment() {

    private lateinit var binding: FragmentBakerStatusBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBakerStatusBinding.inflate(inflater, container, false)
        return binding.root
    }
}