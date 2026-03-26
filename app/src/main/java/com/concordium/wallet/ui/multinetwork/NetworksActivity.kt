package com.concordium.wallet.ui.multinetwork

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.multinetwork.AppNetwork
import com.concordium.wallet.databinding.ActivityNetworksBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.toast.showCustomToast
import kotlinx.coroutines.flow.combine

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
    private lateinit var adapter: NetworkListItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.init(
            shouldRestartOnConnect =
                intent.getBooleanExtra(EXTRA_SHOULD_RESTART_ON_CONNECT, true),
        )

        hideActionBarBack(isVisible = true)
        initEditToggle()
        initViews(
            devModeVisible = intent.getBooleanExtra(
                EXTRA_SHOULD_SHOW_DEV_MODE_LAYOUT,
                false
            )
        )

        subscribeToEvents()
    }

    private fun initEditToggle() {
        combine(
            viewModel.canEdit,
            viewModel.isEditing,
            transform = { canEdit, isEditing ->
                canEdit && !isEditing
            }
        ).collectWhenStarted(this) { isEditVisible ->
            hideEdit(
                isVisible = isEditVisible,
                listener = { viewModel.onEditClicked() },
            )
        }
        viewModel.isEditing.collectWhenStarted(this) { isEditing ->
            hideDone(
                isVisible = isEditing,
                listener = { viewModel.onDoneClicked() },
            )
        }
    }

    private fun initViews(devModeVisible: Boolean) {
        binding.devModeLayout.isVisible = devModeVisible
        binding.devModeLayout.setOnClickListener {
            viewModel.onDevModeClicked()
        }

        adapter = NetworkListItemAdapter(
            onNetworkItemClicked = viewModel::onNetworkItemClicked,
            onAddClicked = viewModel::onAddClicked,
        )
        binding.recyclerview.adapter = adapter
        viewModel.items.collectWhenStarted(this, adapter::setData)
    }

    private fun subscribeToEvents() {
        viewModel.eventsFlow.collectWhenStarted(this) { event ->
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
        viewModel.devModeFlow.collectWhenStarted(this) { isDevModeEnabled ->
            binding.devModeSwitch.isChecked = isDevModeEnabled
            adapter.setIsDevMode(isDevModeEnabled)
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
        private const val EXTRA_SHOULD_SHOW_DEV_MODE_LAYOUT = "should_show_dev_mode_layout"

        fun getBundle(
            shouldRestartOnConnect: Boolean,
            shouldShowDevModeLayout: Boolean
        ): Bundle = Bundle().apply {
            putBoolean(EXTRA_SHOULD_RESTART_ON_CONNECT, shouldRestartOnConnect)
            putBoolean(EXTRA_SHOULD_SHOW_DEV_MODE_LAYOUT, shouldShowDevModeLayout)
        }
    }
}
