package com.concordium.wallet.ui.multinetwork

import android.os.Bundle
import androidx.activity.viewModels
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityEditNetworkBinding
import com.concordium.wallet.ui.base.BaseActivity

class EditNetworkActivity : BaseActivity(
    R.layout.activity_edit_network,
    R.string.network_settings,
) {
    private val binding: ActivityEditNetworkBinding by lazy {
        ActivityEditNetworkBinding.bind(findViewById(R.id.root_layout))
    }
    private val viewModel: EditNetworkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initFields()
    }

    private fun initFields() {
        with(binding.genesisHashField) {
            isEnabled = false
            setText("123456")
        }
    }
}
