package com.concordium.wallet.ui.payandverify

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.sdk.crypto.wallet.web3Id.Statement.IdentityQualifier
import com.concordium.sdk.crypto.wallet.web3Id.UnqualifiedRequest
import com.concordium.wallet.App
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.ui.walletconnect.getIdentityObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigInteger

class DemoPayAndVerifyAccountsViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private var isInitialized = false
    private val accountRepository: AccountRepository by lazy {
        AccountRepository(App.appCore.session.walletStorage.database.accountDao())
    }
    private val proxyRepository: ProxyRepository by lazy {
        ProxyRepository()
    }
    private val _accountItemList: MutableStateFlow<List<DemoPayAndVerifyAccountListItem>?> =
        MutableStateFlow(null)
    val accountItemList = _accountItemList.asStateFlow()
    val isLoading: StateFlow<Boolean> =
        combine(
            accountItemList.map { it == null },
            transform = { values ->
                values.any { it }
            }
        )
            .stateIn(viewModelScope, SharingStarted.Lazily, true)

    fun initializeOnce(
        selectedAccountAddress: String,
        invoice: DemoPayAndVerifyInvoice,
    ) {
        if (isInitialized) {
            return
        }

        viewModelScope.launch {
            loadAccounts(
                invoice = invoice,
                selectedAccountAddress = selectedAccountAddress,
            )
        }

        isInitialized = true
    }

    private suspend fun loadAccounts(
        selectedAccountAddress: String,
        invoice: DemoPayAndVerifyInvoice,
    ) {
        do {
            try {
                val accountsAndIdentities = accountRepository
                    .getAllDoneWithIdentity()

                val cis2PaymentDetails =
                    invoice.paymentDetails as DemoPayAndVerifyInvoice.PaymentDetails.Cis2

                val unqualifiedRequest = UnqualifiedRequest.fromJson(invoice.proofRequestJson)

                val balances = getBalances(
                    addresses = accountsAndIdentities.map { it.account.address },
                    tokenContractIndex = cis2PaymentDetails.tokenContractIndex,
                    tokenId = cis2PaymentDetails.tokenId,
                )

                _accountItemList.emit(
                    accountsAndIdentities.map { (account, identity) ->

                        val balance = balances[account.address] ?: BigInteger.ZERO
                        DemoPayAndVerifyAccountListItem(
                            account = DemoPayAndVerifyAccount(
                                account = account,
                                identity = identity,
                                balance = balance,
                                tokenSymbol = cis2PaymentDetails.tokenSymbol,
                                tokenDecimals = cis2PaymentDetails.tokenDecimals,
                            ),
                            isSelected = selectedAccountAddress == account.address,
                            isValid = balance >= cis2PaymentDetails.amount &&
                                    isValidIdentityForRequest(
                                        identity = identity,
                                        request = unqualifiedRequest,
                                    )
                        )
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) {
                    return
                }

                e.printStackTrace()
                delay(2000)
            }
        } while (_accountItemList.value == null)
    }

    private suspend fun getBalances(
        addresses: List<String>,
        tokenContractIndex: Int,
        tokenId: String,
    ): Map<String, BigInteger> =
        addresses
            .map { address ->
                viewModelScope.async {
                    val balances = proxyRepository.getCIS2TokenBalanceV1(
                        index = tokenContractIndex.toString(),
                        subIndex = "0",
                        accountAddress = address,
                        tokenIds = tokenId,
                    )

                    address to balances.first().balance.toBigInteger()
                }
            }
            .awaitAll()
            .toMap()

    private fun isValidIdentityForRequest(
        identity: Identity,
        request: UnqualifiedRequest,
    ): Boolean {
        val identityObject = getIdentityObject(identity)
        return request.credentialStatements.all { statement ->
            statement.idQualifier is IdentityQualifier
                    && (statement.idQualifier as IdentityQualifier).issuers.contains(identity.identityProviderId.toLong())
                    && statement.canBeProvedBy(identityObject)
        }
    }
}
