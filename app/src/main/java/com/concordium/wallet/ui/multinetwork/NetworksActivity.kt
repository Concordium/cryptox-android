package com.concordium.wallet.ui.multinetwork

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.multinetwork.AppNetwork
import com.concordium.wallet.databinding.ActivityNetworksBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.toast.showCustomToast

class NetworksActivity : BaseActivity(
    R.layout.activity_networks,
    R.string.network_settings,
) {
    private val binding: ActivityNetworksBinding by lazy {
        ActivityNetworksBinding.bind(findViewById(R.id.root_layout))
    }
    private val viewModel: NetworksViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.init(
            isOpenedFromWelcome = intent.getBooleanExtra(IS_OPENED_FROM_WELCOME_EXTRA, false),
        )

        hideActionBarBack(isVisible = true)
        initEditToggle()
        initList()

        subscribeToEvents()
    }

    private fun initEditToggle() {
        viewModel.isEditing.collectWhenStarted(this) { isEditing ->
            if (isEditing) {
                hideEdit(
                    isVisible = false,
                )
                hideDone(
                    isVisible = true,
                    listener = { viewModel.onDoneClicked() },
                )
            } else {
                hideEdit(
                    isVisible = true,
                    listener = { viewModel.onEditClicked() },
                )
                hideDone(
                    isVisible = false,
                )
            }
        }
    }

    private fun initList() {
        val adapter = NetworkListItemAdapter(
            onNetworkItemClicked = viewModel::onNetworkItemClicked,
            onAddClicked = viewModel::onAddClicked,
        )
        binding.recyclerview.adapter = adapter
        viewModel.items.collectWhenStarted(this, adapter::setData)
    }

    private fun subscribeToEvents(

    ) = viewModel.eventsFlow.collectWhenStarted(this) { event ->
        when (event) {
            is NetworksViewModel.Event.GoToEdit -> {
                goToEdit(
                    networkToEdit = event.network,
                )
            }

            NetworksViewModel.Event.RestartOnSuccess -> {
                finishAffinity()
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_SHOW_NETWORK_CONNECTED_TOAST, true)
                )
            }

            NetworksViewModel.Event.FinishOnSuccess -> {
                showCustomToast(
                    title = getString(
                        R.string.template_network_connected,
                        App.appCore.session.network.name,
                    )
                )
                finish()
            }
        }
    }

    private fun goToEdit(networkToEdit: AppNetwork?) {
        startActivity(Intent(this, EditNetworkActivity::class.java))
    }

    override fun loggedOut() {
        // Do nothing as the screen can be opened from the welcome.
    }

    companion object {
        const val IS_OPENED_FROM_WELCOME_EXTRA = "IS_OPENED_FROM_WELCOME_EXTRA"
    }
}
