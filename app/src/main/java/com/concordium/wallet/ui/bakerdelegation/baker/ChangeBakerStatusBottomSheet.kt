package com.concordium.wallet.ui.bakerdelegation.baker

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.annotation.IdRes
import androidx.core.view.forEach
import androidx.fragment.app.setFragmentResult
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentChangeBakingStatusBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ChangeBakerStatusBottomSheet : BottomSheetDialogFragment(
    R.layout.fragment_change_baking_status_bottom_sheet
) {

    override fun getTheme() =
        R.style.AppBottomSheetDialogTheme

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentChangeBakingStatusBottomSheetBinding.bind(view)

        // Set the click listener for all the buttons.
        val resultClickListener = View.OnClickListener { clickedView ->
            setResultAndDismiss(clickedView.id)
        }
        binding.root.forEach { childView ->
            (childView as? Button)?.setOnClickListener(resultClickListener)
        }

        binding.stopButton.isEnabled =
            arguments?.getBoolean(IS_STOP_ENABLED_EXTRA) == true
    }

    private fun setResultAndDismiss(@IdRes clickedButtonId: Int) {
        setFragmentResult(REQUEST_KEY, getResultBundle(clickedButtonId))
        dismiss()
    }

    companion object {
        private const val CLICKED_BUTTON_ID_EXTRA = "clicked_button"
        private const val IS_STOP_ENABLED_EXTRA = "is_stop_enabled"
        const val REQUEST_KEY = "change_baker_status"

        private fun getResultBundle(@IdRes clickedButtonId: Int) = Bundle().apply {
            putInt(CLICKED_BUTTON_ID_EXTRA, clickedButtonId)
        }

        /**
         * @return clicked button ID or null if nothing is clicked.
         */
        @IdRes
        fun getResult(bundle: Bundle): Int? = bundle
            .getInt(CLICKED_BUTTON_ID_EXTRA, 0)
            .takeIf { it != 0 }

        fun newInstance(
            isStopEnabled: Boolean,
        ) = ChangeBakerStatusBottomSheet().apply {
            arguments = Bundle().apply {
                putBoolean(IS_STOP_ENABLED_EXTRA, isStopEnabled)
            }
        }
    }
}
