package com.concordium.wallet.ui.multinetwork

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityNetworksBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity

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
                // TODO
            }

            NetworksViewModel.Event.RestartOnSuccess -> {
                finishAffinity()
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
    }
}
