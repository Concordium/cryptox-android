package com.concordium.wallet.ui.account.accountsoverview

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.lifecycleScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogUnshieldingNoticeBinding
import com.concordium.wallet.ui.more.unshielding.UnshieldingAccountsActivity
import kotlinx.coroutines.delay

class UnshieldingNoticeDialog : AppCompatDialogFragment() {
    override fun getTheme(): Int =
        R.style.CCX_Dialog

    private lateinit var binding: DialogUnshieldingNoticeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogUnshieldingNoticeBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        binding.unshieldButton.setOnClickListener {
            startActivity(Intent(requireActivity(), UnshieldingAccountsActivity::class.java))
            dismiss()
        }

        // Track showing the notice once it is visible to the user.
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            delay(500)
            App.appCore.session.setUnshieldingNoticeShown()
        }
    }

    companion object {
        const val TAG = "unshielding-notice"
    }
}
