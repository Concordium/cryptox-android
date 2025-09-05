package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.concordium.wallet.databinding.FragmentSendTokenBinding

class SendTokenFragment : Fragment() {

    private lateinit var binding: FragmentSendTokenBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSendTokenBinding.inflate(inflater, container, false)
        return binding.root
    }
}