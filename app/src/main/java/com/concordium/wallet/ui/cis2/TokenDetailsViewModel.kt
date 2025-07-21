package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.concordium.wallet.data.model.NewToken
import com.concordium.wallet.data.room.Account
import java.io.Serializable

class TokenDetailsViewModel(application: Application): AndroidViewModel(application) {
    var tokenDetailsData = TokenDetailsData()

}

data class TokenDetailsData(
    var account: Account? = null,
    var selectedToken: NewToken? = null,
    var hasPendingDelegationTransactions: Boolean = false,
    var hasPendingValidationTransactions: Boolean = false,
) : Serializable