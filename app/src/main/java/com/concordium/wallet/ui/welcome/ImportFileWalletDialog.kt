package com.concordium.wallet.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogImportFileWalletBinding
import com.concordium.wallet.ui.more.import.ImportActivity

class ImportFileWalletDialog : AppCompatDialogFragment() {

    override fun getTheme() = R.style.CCX_Dialog

    private lateinit var binding: DialogImportFileWalletBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogImportFileWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        listOf(binding.closeButton, binding.goBackButton).forEach {
            it.setOnClickListener {
                dismiss()
            }
        }

        binding.importButton.setOnClickListener {
            goToImport()
        }
    }

    private fun goToImport() {
        startActivity(
            Intent(requireActivity(), ImportActivity::class.java).apply {
                putExtra(ImportActivity.EXTRA_GO_TO_ACCOUNTS_OVERVIEW_ON_SUCCESS, true)
            }
        )
    }

    companion object {
        const val TAG = "import_file_wallet"
    }
}