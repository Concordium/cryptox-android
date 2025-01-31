package com.concordium.wallet.ui.common.identity

import android.app.Application
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.core.notifications.UpdateNotificationsSubscriptionUseCase
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.model.IdentityTokenContainer
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.ui.cis2.defaults.DefaultFungibleTokensManager
import com.concordium.wallet.ui.cis2.defaults.DefaultTokensManagerFactory
import com.concordium.wallet.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.net.URL

class IdentityUpdater(val application: Application, private val viewModelScope: CoroutineScope) {

    private val gson = App.appCore.gson
    private val identityRepository =
        IdentityRepository(App.appCore.session.walletStorage.database.identityDao())
    private val accountRepository =
        AccountRepository(App.appCore.session.walletStorage.database.accountDao())
    private val recipientRepository =
        RecipientRepository(App.appCore.session.walletStorage.database.recipientDao())
    private val defaultFungibleTokensManager: DefaultFungibleTokensManager
    private val updateNotificationsSubscriptionUseCase by lazy(::UpdateNotificationsSubscriptionUseCase)

    private var updateListener: UpdateListener? = null
    private var run = true

    init {

        val defaultTokensManagerFactory = DefaultTokensManagerFactory(
            contractTokensRepository = ContractTokensRepository(
                App.appCore.session.walletStorage.database.contractTokenDao()
            ),
        )
        defaultFungibleTokensManager = defaultTokensManagerFactory.getDefaultFungibleTokensManager()
    }

    interface UpdateListener {
        fun onError(identity: Identity, account: Account?)
        fun onDone()
        fun onNewAccountFinalized(accountName: String)
    }

    fun setUpdateListener(updateListener: UpdateListener) {
        this.updateListener = updateListener
    }

    fun dispose() {
        // Coroutines is disposed/cancelled when in viewModelScope
    }

    fun stop() {
        run = false
        updateListener = null
        Log.d("Poll for identity status stopped")
    }

    fun checkPendingIdentities(updateListener: UpdateListener) {
        Log.d("Poll for identity status started")
        this.updateListener = updateListener
        run = true
        viewModelScope.launch(Dispatchers.IO) {
            var hasMorePending = true
            while (isActive && hasMorePending && run) {
                hasMorePending = pollForIdentityStatus()
                delay(5000)
            }
            withContext(Dispatchers.Main) {
                updateListener.onDone()
            }
            Log.d("Poll for identity status done")
        }
    }

    private suspend fun pollForIdentityStatus(): Boolean {
        var hasMorePending = false
        Log.d("Poll for identity status")
        for (identity in identityRepository.getAll()) {
            if (identity.status == IdentityStatus.PENDING) {
                try {
                    val resp = URL(identity.codeUri).readText()
                    Log.d("Identity poll response: $resp")

                    val identityTokenContainer = gson.fromJson<IdentityTokenContainer>(
                        resp,
                        IdentityTokenContainer::class.java
                    )

                    val newStatus =
                        if (BuildConfig.FAIL_IDENTITY_CREATION) IdentityStatus.ERROR else identityTokenContainer.status

                    if (newStatus != IdentityStatus.PENDING) {
                        identity.status = identityTokenContainer.status
                        identity.detail = identityTokenContainer.detail

                        if (newStatus == IdentityStatus.DONE && identityTokenContainer.token != null) {
                            val token = identityTokenContainer.token
                            val identityContainer = token.identityObject
                            val accountCredentialWrapper = token.credential
                            val accountAddress = token.accountAddress
                            identity.identityObject = identityContainer.value
                            identityRepository.update(identity)
                            val account =
                                accountRepository.getAllByIdentityId(identity.id).firstOrNull()
                            account?.let {
                                if (it.address == accountAddress) {
                                    // it.credential = CredentialWrapper(RawJson(gson.toJson(CredentialContentWrapper(accountCredentialWrapper.value))), accountCredentialWrapper.v) //Make up for protocol inconsistency
                                    it.credential = accountCredentialWrapper
                                    if (identityTokenContainer.status == IdentityStatus.DONE) {
                                        if (account.transactionStatus != TransactionStatus.FINALIZED) {
                                            updateListener?.onNewAccountFinalized(account.name)

                                            // Add it to the recipient list.
                                            recipientRepository.insert(Recipient(account))

                                            // Add default CIS-2 fungible tokens for it.
                                            defaultFungibleTokensManager.addForAccount(account.address)
                                            updateNotificationsSubscriptionUseCase()
                                        }
                                        account.transactionStatus = TransactionStatus.FINALIZED
                                    } else if (identityTokenContainer.status == IdentityStatus.ERROR) {
                                        account.transactionStatus = TransactionStatus.ABSENT
                                    }
                                    accountRepository.update(account)
                                }
                            }
                        } else if (newStatus == IdentityStatus.ERROR) {

                            identityRepository.update(identity)
                            val account =
                                accountRepository.getAllByIdentityId(identity.id).firstOrNull()
                            account?.let { accountRepository.delete(it) }
                            withContext(Dispatchers.Main) {
                                updateListener?.onError(identity, account)
                            }
                        }
                        if (App.appCore.session.newIdentities[identity.id] == null) {
                            App.appCore.session.newIdentities[identity.id] = identity
                        }
                    } else {
                        hasMorePending = true
                    }
                } catch (e: FileNotFoundException) {
                    Log.e("Identity backend request failed", e)
                    hasMorePending = true
                }
            }
        }
        return hasMorePending
    }
}
