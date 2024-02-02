package com.concordium.wallet.ui.more.dev

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityDevBinding
import com.concordium.wallet.ui.base.BaseActivity

class DevActivity : BaseActivity(R.layout.activity_dev, R.string.app_name) {

    private lateinit var viewModel: DevViewModel
    private val binding by lazy {
        ActivityDevBinding.bind(findViewById(R.id.root_layout))
    }

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        viewModel.initialize()
        initializeViews()
    }

    //endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[DevViewModel::class.java]

        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
    }

    private fun initializeViews() {
        binding.progress.progressLayout.visibility = View.GONE

        binding.createDataButton.setOnClickListener {
            viewModel.createData()
        }

        binding.clearDataButton.setOnClickListener {
            viewModel.clearData()
        }
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

    //endregion
}