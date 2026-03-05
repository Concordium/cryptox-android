package com.concordium.wallet.ui.multinetwork

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
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
    private val editNetworkLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.onEditedSuccessfully()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.init(
            shouldRestartOnConnect =
                intent.getBooleanExtra(EXTRA_SHOULD_RESTART_ON_CONNECT, true),
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
                showConnectionToast()
                finishAffinity()
                startActivity(Intent(this, MainActivity::class.java))
            }

            NetworksViewModel.Event.FinishOnSuccess -> {
                showConnectionToast()
                finish()
            }
        }
    }

    private fun showConnectionToast() {
        showCustomToast(
            title = getString(
                R.string.template_network_connected,
                App.appCore.session.network.name,
            )
        )
    }

    private fun goToEdit(networkToEdit: AppNetwork?) {
        editNetworkLauncher.launch(
            Intent(this, EditNetworkActivity::class.java)
                .putExtras(
                    EditNetworkActivity.getBundle(
                        networkToEditHash = networkToEdit?.genesisHash,
                        shouldRestartOnConnect = viewModel.shouldRestartOnConnect,
                    )
                )
        )
    }

    override fun loggedOut() {
        // Do nothing as the screen can be opened from the welcome.
    }

    companion object {
        private const val EXTRA_SHOULD_RESTART_ON_CONNECT = "should_restart_on_connect"

        fun getBundle(
            shouldRestartOnConnect: Boolean,
        ): Bundle = Bundle().apply {
            putBoolean(EXTRA_SHOULD_RESTART_ON_CONNECT, shouldRestartOnConnect)
        }
    }
}
