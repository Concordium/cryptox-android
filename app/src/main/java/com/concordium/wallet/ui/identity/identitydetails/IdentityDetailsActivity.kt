package com.concordium.wallet.ui.identity.identitydetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.databinding.ActivityIdentityDetailsBinding
import com.concordium.wallet.databinding.DialogIdentityEdittextBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class IdentityDetailsActivity : BaseActivity(
    R.layout.activity_identity_details,
    R.string.identity_details_title,
) {
    companion object {
        const val EXTRA_IDENTITY = "extra_identity"
    }

    private lateinit var binding: ActivityIdentityDetailsBinding
    private lateinit var viewModel: IdentityDetailsViewModel

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdentityDetailsBinding.bind(findViewById(R.id.root_layout))

        val identity = intent.getSerializableExtra(EXTRA_IDENTITY) as Identity
        initializeViewModel()
        viewModel.initialize(identity)
        initViews()
        hideActionBarBack(isVisible = true)
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[IdentityDetailsViewModel::class.java]
    }

    private fun initViews() {
        initializeErrorViews()
        binding.identityView.setIdentityData(viewModel.identity)
        val attributes = viewModel.identity.identityObject!!.attributeList.chosenAttributes

        binding.identityView.enableChangeNameOption(viewModel.identity)

        if (viewModel.identity.status != IdentityStatus.DONE) {
            binding.contentCardview.visibility = View.GONE
        }

        val adapter = IdentityAttributeAdapter(attributes.toSortedMap())
        binding.recyclerview.adapter = adapter
        binding.recyclerview.isNestedScrollingEnabled = false

        viewModel.identityChanged.observe(this) {
            binding.identityView.setIdentityData(it)
        }

        binding.identityView.setOnChangeNameClickListener {
            showChangeNameDialog()
        }
    }

    private fun initializeErrorViews() {
        if (viewModel.identity.status == IdentityStatus.ERROR) {
            binding.errorWrapperLayout.visibility = View.VISIBLE
            binding.errorTextview.text = viewModel.identity.detail ?: ""
            binding.removeButton.setOnClickListener {
                viewModel.removeIdentity(viewModel.identity)
                finish()
            }
        } else {
            binding.errorWrapperLayout.visibility = View.GONE
        }
    }

    private fun showChangeNameDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(getString(R.string.identity_details_change_name_popup_title))
        builder.setMessage(getString(R.string.identity_details_change_name_popup_subtitle))
        val view = DialogIdentityEdittextBinding.inflate(LayoutInflater.from(builder.context))
        val input = view.inputEdittext
        input.setText(viewModel.identity.name)
        input.hint = getString(R.string.identity_details_change_name_hint)
        builder.setView(view.root)
        builder.setPositiveButton(getString(R.string.account_details_change_name_popup_save)) { _, _ ->
            viewModel.changeIdentityName(input.text.toString())
        }
        builder.setNegativeButton(getString(R.string.account_details_change_name_popup_cancel)) { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }
}
