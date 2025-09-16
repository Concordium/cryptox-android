package com.concordium.wallet.ui.more.import

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.databinding.FragmentImportConfirmedBinding
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.uicore.view.ImportResultView

class ImportConfirmedFragment(titleId: Int? = null) : BaseFragment(titleId) {

    private val viewModel: ImportViewModel by activityViewModels()
    private lateinit var binding: FragmentImportConfirmedBinding

    //region Lifecycle
    // ************************************************************

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImportConfirmedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViewModel()
        initializeViews()
    }

    //endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {

        viewModel.waitingLiveData.observe(viewLifecycleOwner) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModel.errorLiveData.observe(viewLifecycleOwner, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
    }

    private fun initializeViews() {
        binding.confirmButton.setOnClickListener {
            viewModel.finishImport(isSuccessful = true)
        }

        val importResult = viewModel.importResult
        binding.titleTextView.setText(
            if (importResult.hasAnyFailed())
                R.string.import_confirmed_header_partially
            else
                R.string.import_confirmed_header
        )

        addImportResultViews()
    }

    private fun addImportResultViews() {
        val importResult = viewModel.importResult
        for (identityImportResult in importResult.identityResultList) {
            val importResultView = ImportResultView(requireActivity())
            importResultView.setIdentityData(identityImportResult)
            addImportResultView(importResultView)
        }

        val importResultView = ImportResultView(requireActivity())
        importResultView.setAddressBookData(importResult)
        addImportResultView(importResultView)
    }

    private fun addImportResultView(importResultView: ImportResultView) {
        binding.importResultLayout.addView(importResultView)
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun showWaiting(waiting: Boolean) {
        // The 'parent' activity is handling the progress_layout
        binding.confirmButton.isEnabled = !waiting
    }

    private fun showError(stringRes: Int) {
        popup.showSnackbar(binding.root, stringRes)
    }

    //endregion
}
