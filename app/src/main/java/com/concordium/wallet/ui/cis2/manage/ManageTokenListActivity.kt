package com.concordium.wallet.ui.cis2.manage

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityManageTokenListBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.TokensViewModel

class ManageTokenListActivity : BaseActivity(
    R.layout.activity_manage_token_list,
    R.string.cis_manage_token_list
) {
    private val binding by lazy {
        ActivityManageTokenListBinding.bind(findViewById(R.id.root_layout))
    }
    lateinit var viewModelTokens: TokensViewModel

    private var manageTokensBottomSheet: ManageTokensBottomSheet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(isVisible = true)
        hideAddContact(isVisible = true) {
            showFindTokensDialog()
        }

        viewModelTokens = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[TokensViewModel::class.java]

        viewModelTokens.updateWithSelectedTokensDone.observe(this) { anyChanges ->
            this.runOnUiThread {
                manageTokensBottomSheet?.dismiss()
                manageTokensBottomSheet = null
                if (anyChanges) {
                    Toast.makeText(
                        this,
                        R.string.cis_tokens_updated,
                        Toast.LENGTH_SHORT
                    ).show()
                } else
                    Toast.makeText(
                        this,
                        R.string.cis_tokens_not_updated,
                        Toast.LENGTH_SHORT
                    ).show()
            }
        }

    }

    private fun showFindTokensDialog() {
        manageTokensBottomSheet = ManageTokensBottomSheet()
        manageTokensBottomSheet?.show(supportFragmentManager, "")
    }
}