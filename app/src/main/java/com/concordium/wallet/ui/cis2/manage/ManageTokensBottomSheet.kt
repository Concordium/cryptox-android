package com.concordium.wallet.ui.cis2.manage

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentManageTokensBottomSheetBinding
import com.concordium.wallet.ui.cis2.TokensFragment
import com.concordium.wallet.ui.cis2.TokensViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ManageTokensBottomSheet : BottomSheetDialogFragment(
    R.layout.fragment_manage_tokens_bottom_sheet
) {
    override fun getTheme() =
        R.style.AppBottomSheetDialogTheme

    private var _binding: FragmentManageTokensBottomSheetBinding? = null
    private val binding get() = _binding!!
    val viewModel: TokensViewModel
        get() = (parentFragment as TokensFragment).viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentManageTokensBottomSheetBinding.bind(view)
        initViews()
        initObservers()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        bottomSheetDialog.setOnShowListener {
            val targetHeight = (Resources.getSystem().displayMetrics.heightPixels * 0.9).toInt()
            bottomSheetDialog.behavior.peekHeight = targetHeight
            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

            // Make the sheet fill 90% of height
            bottomSheetDialog.findViewById<View>(R.id.content_layout)?.updateLayoutParams {
                height = targetHeight
            }

            // Handle back button press.
            bottomSheetDialog.onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        viewModel.stepPage(-1)
                    }
                }
            )
        }
        return bottomSheetDialog
    }

    override fun onResume() {
        super.onResume()
        binding.viewPager.currentItem = 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.onFindTokensDialogDismissed()
    }

    private fun initViews() {
        binding.viewPager.adapter = LookForNewTokensAdapter()
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.currentItem = 0
    }

    private fun initObservers() {
        viewModel.lookForTokens.observe(viewLifecycleOwner) {
            if (it != TokensViewModel.TOKENS_NOT_LOADED
                && viewModel.tokens.isNotEmpty()
                && binding.viewPager.currentItem == 0
            ) {
                binding.viewPager.currentItem++
            }
        }
        viewModel.stepPageBy.observe(viewLifecycleOwner) {
            val targetPosition = binding.viewPager.currentItem + it

            if (targetPosition == -1) {
                dismiss()
                return@observe
            }

            if (targetPosition >= 0 && targetPosition < (binding.viewPager.adapter?.itemCount
                    ?: 0)
            ) {
                binding.viewPager.currentItem = targetPosition
            }

            if (binding.viewPager.currentItem == 0) {
                viewModel.tokens.clear()
            }
        }
    }

    private inner class LookForNewTokensAdapter : FragmentStateAdapter(this) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ManageTokensContractAddressFragment()
                1 -> ManageTokensSelectionFragment()
                2 -> ManageTokensTokenDetailsFragment()
                else -> throw IndexOutOfBoundsException("Unsupported fragment position $position")
            }
        }
    }
}
