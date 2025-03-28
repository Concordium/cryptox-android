package com.concordium.wallet.ui.multiwallet

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.databinding.ActivityWalletsBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity

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
        initActionConfirmation()
        initRemoval()
        subscribeToEvents()

        hideActionBarBack(isVisible = true)
        hideInfo(isVisible = true) {
            WalletsAddingBottomSheet()
                .showSingle(supportFragmentManager, WalletsAddingBottomSheet.TAG)
        }
        hideRightPlus(
            isVisible = BuildConfig.DEBUG,
            hasNotice = true,
        ) {
            Toast
                .makeText(
                    this,
                    "This button is for devs, it is not present in production",
                    Toast.LENGTH_LONG
                )
                .show()
            WalletsActionConfirmationDialog
                .addingSeedWallet()
                .showSingle(supportFragmentManager, WalletsActionConfirmationDialog.TAG)
        }
    }

    private fun initList() {
        val adapter = WalletListItemAdapter(
            onWalletClicked = viewModel::onWalletItemClicked,
            onAddClicked = this::onAddClicked,
        )
        binding.recyclerview.adapter = adapter
        viewModel.listItemsLiveData.observe(this, adapter::setData)
    }

    private fun initActionConfirmation() {
        supportFragmentManager.setFragmentResultListener(
            WalletsActionConfirmationDialog.CONFIRMATION_REQUEST,
            this
        ) { _, bundle ->
            val confirmedAction = WalletsActionConfirmationDialog.getResult(bundle)
                .takeIf(WalletsActionConfirmationDialog.Result::isConfirmed)
                ?.action
                ?: return@setFragmentResultListener

            when (confirmedAction) {
                WalletsActionConfirmationDialog.ADDING_SEED_WALLET_ACTION ->
                    SeedWalletAddingBottomSheet()
                        .showSingle(supportFragmentManager, SeedWalletAddingBottomSheet.TAG)

                WalletsActionConfirmationDialog.ADDING_FILE_WALLET_ACTION ->
                    viewModel.onAddingFileWalletConfirmed()

                WalletsActionConfirmationDialog.REMOVING_WALLET_ACTION ->
                    viewModel.onRemovingWalletConfirmed()
            }
        }

        supportFragmentManager.setFragmentResultListener(
            SeedWalletAddingBottomSheet.ACTION_REQUEST,
            this
        ) { _, bundle ->
            val confirmedAction = SeedWalletAddingBottomSheet.getResult(bundle)
            viewModel.onAddingSeedWalletConfirmed(
                import = confirmedAction == SeedWalletAddingBottomSheet.ChosenAction.IMPORT,
            )
        }
    }

    private fun initRemoval() {
        viewModel.isRemoveButtonVisibleLiveData.observe(
            this,
            binding.removeButton::isVisible::set
        )
        viewModel.removeButtonWalletTypeLiveData.observe(this) { walletType ->
            binding.removeButtonTextView.setText(
                when (walletType!!) {
                    AppWallet.Type.FILE ->
                        R.string.wallets_remove_file_wallet

                    AppWallet.Type.SEED ->
                        R.string.wallets_remove_seed_wallet
                }
            )
        }
        binding.removeButton.setOnClickListener {
            WalletsActionConfirmationDialog
                .removingWallet()
                .showSingle(supportFragmentManager, WalletsActionConfirmationDialog.TAG)
        }
    }

    private fun subscribeToEvents(
    ) = viewModel.eventsFlow.collectWhenStarted(this) { event ->
        when (event) {
            is WalletsViewModel.Event.GoToMain -> {
                finishAffinity()
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_IMPORT_FROM_FILE, event.startWithFileImport)
                        .putExtra(MainActivity.EXTRA_IMPORT_FROM_SEED, event.startWithSeedImport)
                )
            }
        }
    }

    private fun onAddClicked(walletType: AppWallet.Type) = when (walletType) {
        AppWallet.Type.FILE ->
            WalletsActionConfirmationDialog
                .addingFileWallet()
                .showSingle(supportFragmentManager, WalletsActionConfirmationDialog.TAG)

        AppWallet.Type.SEED ->
            WalletsActionConfirmationDialog
                .addingSeedWallet()
                .showSingle(supportFragmentManager, WalletsActionConfirmationDialog.TAG)
    }
}
