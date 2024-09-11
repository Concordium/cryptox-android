package com.concordium.wallet.ui.seed.recoverprocess

import android.content.Context
import androidx.fragment.app.Fragment
import com.concordium.wallet.ui.seed.recoverprocess.RecoverProcessViewModel.Companion.RECOVER_PROCESS_DATA

abstract class RecoverProcessBaseFragment : Fragment() {
    protected lateinit var recoverProcessData: RecoverProcessData

    override fun onAttach(context: Context) {
        super.onAttach(context)
        arguments?.getSerializable(RECOVER_PROCESS_DATA)?.let {
            recoverProcessData = it as RecoverProcessData
        }
    }
}
