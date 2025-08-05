package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.core.tokens.TokensInteractor
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.PLTToken
import com.concordium.wallet.data.room.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.Serializable

class TokenDetailsViewModel(application: Application): AndroidViewModel(application), KoinComponent {
    private val tokensInteractor by inject<TokensInteractor>()

    var tokenDetailsData = TokenDetailsData()

    fun deleteSelectedToken() = viewModelScope.launch(Dispatchers.IO) {
        tokensInteractor.deleteToken(
            tokenDetailsData.account?.address ?: return@launch,
            tokenDetailsData.selectedToken ?: return@launch
        )
    }

    fun unmarkNewlyReceivedSelectedToken() = viewModelScope.launch(Dispatchers.IO) {
        tokensInteractor.unmarkNewlyReceivedToken(
            tokenDetailsData.selectedToken ?: return@launch
        )
    }

    fun tokenSymbol(): String {
        return if (tokenDetailsData.selectedToken is PLTToken) {
            (tokenDetailsData.selectedToken as PLTToken).tokenId
        } else {
            tokenDetailsData.selectedToken?.metadata?.symbol ?:
            tokenDetailsData.selectedToken?.metadata?.name ?: ""
        }
    }
}

data class TokenDetailsData(
    var account: Account? = null,
    var selectedToken: Token? = null,
    var hasPendingDelegationTransactions: Boolean = false,
    var hasPendingValidationTransactions: Boolean = false,
) : Serializable