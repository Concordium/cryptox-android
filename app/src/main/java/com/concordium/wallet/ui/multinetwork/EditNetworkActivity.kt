package com.concordium.wallet.ui.multinetwork

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
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
        }

        with(binding.walletProxyUrlField) {
            setInputType(EditorInfo.TYPE_TEXT_VARIATION_URI)
        }

        with(binding.genesisHashField) {
            isEnabled = false
            setText("123456")
        }

        with(binding.ccdscanUrlField) {
            setInputType(EditorInfo.TYPE_TEXT_VARIATION_URI)
        }

        with(binding.notificationsServiceUrlField) {
            setInputType(EditorInfo.TYPE_TEXT_VARIATION_URI)
        }
    }

    private fun initSaveButton() {
        viewModel.canSave.collectWhenStarted(this, binding.saveButton::setEnabled)
    }
}
