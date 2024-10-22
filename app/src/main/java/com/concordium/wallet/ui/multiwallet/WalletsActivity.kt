package com.concordium.wallet.ui.multiwallet

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityWalletsBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.welcome.ImportSeedPhraseInfoBottomSheet

class WalletsActivity : BaseActivity(
    R.layout.activity_wallets,
    R.string.wallets_title,
) {
    private val binding: ActivityWalletsBinding by lazy {
        ActivityWalletsBinding.bind(findViewById(R.id.root_layout))
    }
    private val viewModel: WalletsViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initList()

        hideActionBarBack(isVisible = true)
        hideInfo(isVisible = true) {
            showAddingBottomSheet()
        }
    }

    private fun initList() {
        val adapter = WalletListItemAdapter(
            onWalletClicked = {
                Toast.makeText(this, it.source.toString(), Toast.LENGTH_SHORT).show()
            },
            onAddClicked = {
                Toast.makeText(this, "Add ${it.walletType} wallet", Toast.LENGTH_SHORT).show()
            }
        )
        binding.recyclerview.adapter = adapter
        viewModel.listItemsLiveData.observe(this, adapter::setData)
    }

    private fun showAddingBottomSheet() {
        WalletsAddingBottomSheet().showSingle(
            supportFragmentManager,
            WalletsAddingBottomSheet.TAG
        )
    }
}
