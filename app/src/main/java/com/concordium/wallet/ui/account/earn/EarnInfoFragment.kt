package com.concordium.wallet.ui.account.earn

import androidx.fragment.app.Fragment
import com.concordium.wallet.databinding.FragmentEarnInfoBinding

class EarnInfoFragment: Fragment() {

    private lateinit var binding: FragmentEarnInfoBinding

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): android.view.View {
        binding = FragmentEarnInfoBinding.inflate(inflater, container, false)
        return binding.root
    }
}