package com.concordium.wallet.ui.account.accountqrcode

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.FragmentReceiveBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.uicore.toast.ToastType
import com.concordium.wallet.uicore.toast.showCustomToast
import com.concordium.wallet.util.ClipboardUtil
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ReceiveFragment : Fragment() {

    private lateinit var binding: FragmentReceiveBinding

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity())[MainViewModel::class.java]
    }
    private val viewModel: ReceiveViewModel by viewModel {
        parametersOf(mainViewModel)
    }
    private val qrDrawableFactory by lazy(::QrDrawableFactory)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentReceiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        viewModel.activeAccount.collectWhenStarted(viewLifecycleOwner) {
            binding.shareLayout.isEnabled = it != null
            binding.copyAddressLayout.isEnabled = it != null
            it?.let(::updateViews)
        }
    }

    private fun initViews() {
        binding.shareLayout.setOnClickListener {
            shareAddress()
        }
        binding.copyAddressLayout.setOnClickListener {
            copyAddress()
        }
    }

    private fun updateViews(
        account: Account,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val qrDrawable = qrDrawableFactory.getDrawable(
                content = account.address,
                squareColor = Color.WHITE,
                errorCorrectionLevel = ErrorCorrectionLevel.L,
            )
            binding.addressQrImageview.setImageDrawable(qrDrawable)
        }
        binding.accountTitleTextview.text = account.getAccountName()
        binding.addressQrTextview.text = account.address
    }

    private fun shareAddress() {
        val sendIntent = Intent(Intent.ACTION_SEND)
        sendIntent.type = "text/plain"
        sendIntent.putExtra(Intent.EXTRA_TEXT, viewModel.getAccount()!!.address)
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun copyAddress() {
        ClipboardUtil.copyToClipboard(
            context = requireContext(),
            text = viewModel.getAccount()!!.address
        )

        requireActivity().showCustomToast(
            title = getString(R.string.account_qr_code_copied),
            toastType = ToastType.INFO
        )
    }
}
