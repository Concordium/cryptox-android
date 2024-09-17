package com.concordium.wallet.ui.seed.recover.private_key

import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentPrivateKeyRecoverInputBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.util.Log
import kotlinx.coroutines.flow.update

class PrivateKeyRecoverInputFragment : Fragment() {
    private var _binding: FragmentPrivateKeyRecoverInputBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PrivateKeyRecoverViewModel
        get() = (requireActivity() as RecoverPrivateKeyWalletActivity).viewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrivateKeyRecoverInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateViews()
        observePrivateKey()
    }

    private fun updateViews() {
        binding.etTitle.doOnTextChanged { text, _, _, _ ->
            viewModel.privateKey.value = text.toString()
            viewModel.validatePrivateKey()
        }
        binding.importInputActionButton.setOnClickListener {
            if (viewModel.privateKey.value.isEmpty()) {
                pasteFromClipboard()
            } else {
                viewModel.clearPrivateKey()
                binding.etTitle.setText("")
            }
        }
    }

    private fun observePrivateKey() {
        viewModel.privateKey.collectWhenStarted(viewLifecycleOwner) {
            binding.importInputActionButton.text =
                if (it.isEmpty()) getString(R.string.private_key_recover_paste)
                else getString(R.string.private_key_recover_clear)
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
        viewModel.privateKey.value = clipboard
        viewModel.validatePrivateKey()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}