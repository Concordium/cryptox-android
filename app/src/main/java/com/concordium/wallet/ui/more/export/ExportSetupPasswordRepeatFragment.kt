package com.concordium.wallet.ui.more.export

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.activityViewModels
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentExportSetupPasswordBinding
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.util.KeyboardUtil

class ExportSetupPasswordRepeatFragment(val titleId: Int? = null) : BaseFragment(titleId) {

    private val viewModel: ExportViewModel by activityViewModels()
    private lateinit var binding: FragmentExportSetupPasswordBinding

    //region Lifecycle
    // ************************************************************

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExportSetupPasswordBinding.inflate(inflater, container, false)
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
    }

    private fun initializeViews() {
        binding.instructionTextview.setText(R.string.export_setup_password_repeat_info)
        binding.confirmButton.setOnClickListener {
            onConfirmClicked()
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

    private fun onConfirmClicked() {
        viewModel.checkExportPassword(binding.passwordEdittext.text.toString())
    }

    //endregion
}
