package com.concordium.wallet.ui.multinetwork

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityEditNetworkBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity

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

        hideActionBarBack(isVisible = true)
        initFields()
        initSaveButton()
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
        }
        viewModel.notificationsServiceUrlError.collectWhenStarted(this) { notificationsServiceUrlError ->
            binding.notificationsServiceUrlError.isVisible = notificationsServiceUrlError != null
            binding.notificationsServiceUrlError.text = notificationsServiceUrlError?.message
        }
    }

    private fun initSaveButton() {
        viewModel.canSave.collectWhenStarted(this, binding.saveButton::setEnabled)
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
}
