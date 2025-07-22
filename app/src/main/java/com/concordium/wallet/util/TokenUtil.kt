package com.concordium.wallet.util

import com.concordium.wallet.data.model.PLTToken
import com.concordium.wallet.ui.plt.PLTListStatus

object TokenUtil {

    fun getPLTPLTListStatus(token: PLTToken): PLTListStatus = when {
        (token.isInDenyList != null && token.isInDenyList) -> PLTListStatus.ON_DENY_LIST
        (token.isInAllowList != null && token.isInAllowList) -> PLTListStatus.ON_ALLOW_LIST
        token.isInAllowList != null -> PLTListStatus.NOT_ON_ALLOW_LIST
        else -> PLTListStatus.UNKNOWN
    }
}