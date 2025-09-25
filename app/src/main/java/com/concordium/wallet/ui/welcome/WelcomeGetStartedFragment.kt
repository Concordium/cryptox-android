package com.concordium.wallet.ui.welcome

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentWelcomeGetStartedBinding
import com.concordium.wallet.uicore.handleUrlClicks

class WelcomeGetStartedFragment : Fragment(R.layout.fragment_welcome_get_started) {

    private val binding: FragmentWelcomeGetStartedBinding by lazy {
        FragmentWelcomeGetStartedBinding.bind(requireView())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
    }

    private fun initViews() {
        binding.termsTextView.handleUrlClicks { url ->
            when (url) {
                "#terms" ->
                    startActivity(Intent(requireContext(), WelcomeTermsActivity::class.java))

                else -> {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    ContextCompat.startActivity(requireContext(), browserIntent, null)
                }
            }
        }

        binding.termsCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                App.appCore.tracker.welcomeTermAndConditionsCheckBoxChecked()
            }
            binding.createWalletButton.isEnabled = isChecked
            binding.importWalletButton.isEnabled = isChecked
        }

        binding.createWalletButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                Bundle().apply {
                    putSerializable(CHOSEN_ACTION_EXTRA, ChosenAction.CREATE)
                }
            )
        }

        binding.importWalletButton.setOnClickListener {
            setFragmentResult(
                ACTION_REQUEST,
                Bundle().apply {
                    putSerializable(CHOSEN_ACTION_EXTRA, ChosenAction.IMPORT)
                }
            )
        }
    }

    enum class ChosenAction {
        CREATE,
        IMPORT,
        ;
    }

    companion object {

        const val ACTION_REQUEST = "get-started-action-request"
        private const val CHOSEN_ACTION_EXTRA = "chosen-action"

        @Suppress("DEPRECATION")
        fun getChosenAction(result: Bundle): ChosenAction =
            result.getSerializable(CHOSEN_ACTION_EXTRA) as ChosenAction
    }
}
