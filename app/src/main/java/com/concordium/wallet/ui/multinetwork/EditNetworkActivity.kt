package com.concordium.wallet.ui.multinetwork

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityEditNetworkBinding
import com.concordium.wallet.extension.collect
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.toast.ToastType
import com.concordium.wallet.uicore.toast.showCustomToast

class EditNetworkActivity : BaseActivity(
    R.layout.activity_edit_network,
    R.string.custom_network,
) {
    private val binding: ActivityEditNetworkBinding by lazy {
        ActivityEditNetworkBinding.bind(findViewById(R.id.root_layout))
    }
    private val viewModel: EditNetworkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.init(
            networkToEditHash = intent.getStringExtra(EXTRA_NETWORK_TO_EDIT_HASH),
            shouldRestartOnConnect = intent.getBooleanExtra(EXTRA_SHOULD_RESTART_ON_CONNECT, true),
        )

        setActionBarTitle(
            if (viewModel.networkToEdit != null)
                R.string.edit_network
            else
                R.string.custom_network
        )
        hideActionBarBack(isVisible = true)
        initFields()
        initSaveButton()
        subscribeToEvents()
    }

    private fun initFields() {
        with(binding.networkNameField) {
            setInputType(EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS)
            setTextChangeListener {
                viewModel.onNetworkNameChanged(it?.toString() ?: "")
            }
            setOnFocusChangeListener { isFocused ->
                if (!isFocused) {
                    viewModel.onNetworkNameLostFocus()
                }
            }
            if (viewModel.networkToEdit != null) {
                setText(viewModel.networkToEdit!!.name)
            }
        }
        viewModel.nameError.collectWhenStarted(this) { nameError ->
            binding.networkNameError.isVisible = nameError != null
            binding.networkNameError.text = nameError?.message
        }

        with(binding.walletProxyUrlField) {
            setInputType(EditorInfo.TYPE_TEXT_VARIATION_URI)
            setTextChangeListener {
                viewModel.onWalletProxyUrlChanged(it?.toString() ?: "")
            }
            setOnFocusChangeListener { isFocused ->
                if (!isFocused) {
                    viewModel.onWalletProxyUrlLostFocus()
                }
            }
            if (viewModel.networkToEdit != null) {
                setText(viewModel.networkToEdit!!.walletProxyUrl.toString())
            }
        }
        viewModel.walletProxyUrlError.collectWhenStarted(this) { walletProxyUrlError ->
            binding.walletProxyUrlError.isVisible = walletProxyUrlError != null
            binding.walletProxyUrlError.text = walletProxyUrlError?.message
        }

        with(binding.genesisHashField) {
            isEnabled = false
            viewModel.loadedGenesisHash.collectWhenStarted(this@EditNetworkActivity) { genesisHash ->
                setText(genesisHash ?: "")
            }
        }

        with(binding.ccdscanUrlField) {
            setInputType(EditorInfo.TYPE_TEXT_VARIATION_URI)
            setTextChangeListener {
                viewModel.onCcdScanUrlChanged(it?.toString() ?: "")
            }
            setOnFocusChangeListener { isFocused ->
                if (!isFocused) {
                    viewModel.onCcdScanUrlLostFocus()
                }
            }
            if (viewModel.networkToEdit != null) {
                setText(viewModel.networkToEdit!!.ccdScanFrontendUrl?.toString() ?: "")
            }
        }
        viewModel.ccdScanUrlError.collectWhenStarted(this) { ccdScanUrlError ->
            binding.ccdscanUrlError.isVisible = ccdScanUrlError != null
            binding.ccdscanUrlError.text = ccdScanUrlError?.message
        }

        with(binding.notificationsServiceUrlField) {
            setInputType(EditorInfo.TYPE_TEXT_VARIATION_URI)
            setTextChangeListener {
                viewModel.onNotificationsServiceUrlChanged(it?.toString() ?: "")
            }
            setOnFocusChangeListener { isFocused ->
                if (!isFocused) {
                    viewModel.onNotificationsServiceUrlLostFocus()
                }
            }
            if (viewModel.networkToEdit != null) {
                setText(viewModel.networkToEdit!!.notificationsServiceUrl?.toString() ?: "")
            }
        }
        viewModel.notificationsServiceUrlError.collectWhenStarted(this) { notificationsServiceUrlError ->
            binding.notificationsServiceUrlError.isVisible = notificationsServiceUrlError != null
            binding.notificationsServiceUrlError.text = notificationsServiceUrlError?.message
        }
    }

    private fun initSaveButton() {
        viewModel.canSave.collectWhenStarted(this, binding.saveButton::setEnabled)
        binding.saveButton.setOnClickListener {
            viewModel.onSaveClicked()
        }
    }

    private fun subscribeToEvents(

    ) = viewModel.eventsFlow.collect(lifecycleScope) { event ->
        when (event) {
            is EditNetworkViewModel.Event.FinishOnAdding -> {
                showCustomToast(
                    title = getString(
                        R.string.template_network_added,
                        event.addedNetworkName,
                    )
                )
                finish()
            }

            is EditNetworkViewModel.Event.FinishAfterEdited -> {
                showCustomToast(
                    title = getString(
                        R.string.template_network_saved,
                        event.editedNetworkName,
                    )
                )
                finish()
            }

            is EditNetworkViewModel.Event.RestartAfterEdited -> {
                showCustomToast(
                    title = getString(
                        R.string.template_network_saved,
                        event.editedNetworkName,
                    )
                )
                finishAffinity()
                startActivity(Intent(this, MainActivity::class.java))
            }

            is EditNetworkViewModel.Event.ShowFloatingError -> {
                showCustomToast(
                    title = event.error.message,
                    toastType = ToastType.ERROR,
                )
            }
        }
    }

    private val EditNetworkViewModel.Error.message: String
        get() = when (this) {
            is EditNetworkViewModel.Error.BackendError ->
                getString(stringRes)

            EditNetworkViewModel.Error.InvalidUrl ->
                getString(R.string.error_networks_invalid_url)

            is EditNetworkViewModel.Error.NetworkAlreadyExists ->
                getString(
                    R.string.template_error_network_already_exists,
                    existingNetwork.name,
                )

            EditNetworkViewModel.Error.GenericError ->
                getString(R.string.app_error_general)
        }

    override fun loggedOut() {
        // Do nothing as the screen can be opened from the welcome.
    }

    companion object {
        private const val EXTRA_NETWORK_TO_EDIT_HASH = "network_to_edit_hash"
        private const val EXTRA_SHOULD_RESTART_ON_CONNECT = "should_restart_on_connect"

        /**
         * @param networkToEditHash genesis hash of an existing network to edit,
         * otherwise a new network is added
         */
        fun getBundle(
            networkToEditHash: String?,
            shouldRestartOnConnect: Boolean,
        ): Bundle = Bundle().apply {
            putString(EXTRA_NETWORK_TO_EDIT_HASH, networkToEditHash)
            putBoolean(EXTRA_SHOULD_RESTART_ON_CONNECT, shouldRestartOnConnect)
        }
    }
}
