package com.concordium.wallet.ui.account.accountsoverview

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.ui.more.unshielding.UnshieldingAccountsActivity
import com.concordium.wallet.uicore.dialog.BaseDialogFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UnshieldingNoticeDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(
            title = getString(R.string.unshielding_notice_title),
            description = getString(R.string.unshielding_notice_message),
            okButtonText = getString(R.string.unshielding_notice_unshield),
            iconResId = R.drawable.mw24_shielding
        )

        binding.okButton.setOnClickListener {
            startActivity(Intent(requireActivity(), UnshieldingAccountsActivity::class.java))
            dismiss()
        }

        // Track showing the notice once it is visible to the user.
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                delay(500)
                App.appCore.session.setUnshieldingNoticeShown()
            }
        }
    }

    companion object {
        const val TAG = "unshielding-notice"
    }
}
