package com.concordium.wallet.ui.seed.recover.seed

import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentSeedRecoverInputBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.util.Log

class SeedRecoverInputFragment : Fragment() {
    private var _binding: FragmentSeedRecoverInputBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SeedRecoverViewModel
        get() = (requireActivity() as RecoverSeedWalletActivity).viewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeedRecoverInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateViews()
        observeSeed()
    }

    private fun updateViews() {
        binding.etTitle.doOnTextChanged { text, _, _, _ ->
            viewModel.seed.value = text.toString()
            viewModel.validateSeed()
        }
        binding.importInputActionButton.setOnClickListener {
            if (viewModel.seed.value.isEmpty()) {
                pasteFromClipboard()
            } else {
                viewModel.clearSeed()
                binding.etTitle.setText("")
            }
        }
    }

    private fun observeSeed() {
        viewModel.seed.collectWhenStarted(viewLifecycleOwner) {
            binding.importInputActionButton.text =
                if (it.isEmpty()) getString(R.string.seed_recover_paste)
                else getString(R.string.seed_recover_clear)
        }
    }

    private fun pasteFromClipboard() {
        val clipboard = requireContext().getSystemService(ClipboardManager::class.java)
            ?.primaryClip
            ?.getItemAt(0)
            ?.text
            ?.toString()
            ?.trim()

        if (clipboard.isNullOrEmpty()) {
            Log.d("clipboard_text_blank")
            return
        }

        binding.etTitle.setText(clipboard)
        viewModel.seed.value = clipboard
        viewModel.validateSeed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}