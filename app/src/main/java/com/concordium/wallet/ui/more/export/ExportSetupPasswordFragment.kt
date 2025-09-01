package com.concordium.wallet.ui.more.export

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.databinding.FragmentExportSetupPasswordBinding
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.KeyboardUtil

class ExportSetupPasswordFragment(val titleId: Int? = null) : BaseFragment(titleId) {

    private val viewModel: ExportViewModel by activityViewModels()
    private lateinit var binding: FragmentExportSetupPasswordBinding

    //region Lifecycle
    // ************************************************************

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExportSetupPasswordBinding.inflate(layoutInflater, container, false)
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

        viewModel.errorPasswordLiveData.observe(
            viewLifecycleOwner,
            object : EventObserver<Boolean>() {
                override fun onUnhandledEvent(value: Boolean) {
                    if (value) {
                        showPasswordError()
                    }
                }
            })

        viewModel.errorNonIdenticalRepeatPasswordLiveData.observe(
            viewLifecycleOwner,
            object : EventObserver<Boolean>() {
                override fun onUnhandledEvent(value: Boolean) {
                    if (!value) {
                        // Failure
                        binding.passwordEdittext.setText("")
                        binding.errorTextview.isVisible = true
                        binding.errorTextview.setText(R.string.export_error_entries_different)
                    }
                }
            })
    }

    private fun initializeViews() {
        binding.confirmButton.setOnClickListener {
            onConfirmClicked()
        }
        binding.confirmButton.isEnabled = false
        binding.passwordEdittext.afterTextChanged {
            binding.errorTextview.isVisible = false
            binding.errorTextview.text = ""
            binding.confirmButton.isEnabled =
                viewModel.checkPasswordRequirements(binding.passwordEdittext.text.toString())
        }
        binding.passwordEdittext.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    if (viewModel.checkPasswordRequirements(binding.passwordEdittext.text.toString())) {
                        onConfirmClicked()
                    }
                    true
                }

                else -> false
            }
        }

        Handler(Looper.getMainLooper()).post {
            KeyboardUtil.showKeyboard(
                requireContext(),
                binding.passwordEdittext
            )
        }
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun onConfirmClicked() {
        if (viewModel.checkPasswordRequirements(binding.passwordEdittext.text.toString())) {
            viewModel.setStartExportPassword(binding.passwordEdittext.text.toString())
        } else {
            binding.passwordEdittext.setText("")
            binding.errorTextview.setText(R.string.export_error_password_not_valid)
            binding.errorTextview.isVisible = true
        }
    }

    private fun showPasswordError() {
        binding.passwordEdittext.setText("")
        KeyboardUtil.hideKeyboard(requireActivity())
        popup.showSnackbar(binding.root, R.string.export_error_password_setup)
    }

    //endregion
}
