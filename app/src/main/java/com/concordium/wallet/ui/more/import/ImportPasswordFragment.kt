package com.concordium.wallet.ui.more.import

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
import com.concordium.wallet.databinding.FragmentImportPasswordBinding
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.KeyboardUtil

class ImportPasswordFragment(titleId: Int? = null) : BaseFragment(titleId) {

    private val viewModel: ImportViewModel by activityViewModels()
    private lateinit var binding: FragmentImportPasswordBinding

    //region Lifecycle
    // ************************************************************

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImportPasswordBinding.inflate(inflater, container, false)
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
                    onConfirmClicked()
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

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.passwordEdittext.isEnabled = false
            binding.confirmButton.isEnabled = false
        } else {
            binding.passwordEdittext.isEnabled = true
            binding.confirmButton.isEnabled = !binding.passwordEdittext.text.isNullOrEmpty()
        }
    }

    private fun onConfirmClicked() {
        if (viewModel.checkPasswordRequirements(binding.passwordEdittext.text.toString())) {
            viewModel.startImport(binding.passwordEdittext.text.toString())
        } else {
            binding.passwordEdittext.setText("")
            binding.errorTextview.isVisible = true
            binding.errorTextview.setText(R.string.export_error_password_not_valid)
        }
    }

    //endregion
}
