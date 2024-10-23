package com.concordium.wallet.ui.multiwallet

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.concordium.wallet.R
import com.concordium.wallet.core.multiwallet.AppWallet

class WalletSwitchView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val activeWalletTextView: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.view_wallet_switch, this, true)
        background = ContextCompat.getDrawable(context, R.drawable.wallet_switch_background)
        activeWalletTextView = findViewById(R.id.active_wallet_text_view)
    }

    fun bind(viewModel: WalletSwitchViewModel) {
        val lifecycleOwner = findViewTreeLifecycleOwner()
            ?: error("Can't bind before the view has the lifecycle owner")

        viewModel.isViewVisibleLiveData.observe(lifecycleOwner, ::isVisible::set)
        viewModel.activeWalletTypeLiveData.observe(lifecycleOwner) { activeWalletType ->
            activeWalletTextView.setText(
                when (activeWalletType!!) {
                    AppWallet.Type.FILE ->
                        R.string.wallet_switch_file_wallet_selected

                    AppWallet.Type.SEED ->
                        R.string.wallet_switch_seed_wallet_selected
                }
            )
        }

        setOnClickListener {
            viewModel.onSwitchClicked()
        }
    }
}
