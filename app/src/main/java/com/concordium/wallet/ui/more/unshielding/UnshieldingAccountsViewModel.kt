package com.concordium.wallet.ui.more.unshielding

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.ShieldedAccountEncryptionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.account.common.accountupdater.TotalBalancesData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigInteger

class UnshieldingAccountsViewModel(application: Application) : AndroidViewModel(application) {
    private val accountRepository: AccountRepository by lazy {
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        AccountRepository(accountDao)
    }
    private val accountUpdater: AccountUpdater by lazy {
        AccountUpdater(application, viewModelScope)
    }
    private val unshieldedAmountsByAccount = mutableMapOf<String, BigInteger>()

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean> = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>> = _errorLiveData

    private val _listItemsLiveData = MutableLiveData<List<UnshieldingAccountListItem>>()
    val listItemsLiveData: LiveData<List<UnshieldingAccountListItem>> = _listItemsLiveData

    private val _showAuthLiveData = MutableLiveData<Event<Boolean>>()
    val showAuthLiveData: LiveData<Event<Boolean>> = _showAuthLiveData

    private val _openUnshieldingLiveData = MutableLiveData<Event<String>>()
    val openUnshieldingLiveData: LiveData<Event<String>> = _openUnshieldingLiveData

    private val _isDoneButtonVisibleLiveData = MutableLiveData(false)
    val isDoneButtonVisibleLiveData: LiveData<Boolean> = _isDoneButtonVisibleLiveData

    private lateinit var accountToUnshield: Account

    init {
        findAccountsMayNeedUnshielding()

        _listItemsLiveData.observeForever { listItems ->
            _isDoneButtonVisibleLiveData.value =
                listItems.all(UnshieldingAccountListItem::isUnshielded)
        }
    }

    private fun findAccountsMayNeedUnshielding() = viewModelScope.launch(Dispatchers.IO) {
        _waitingLiveData.postValue(true)

        val allDoneAccounts = accountRepository.getAllDone()

        // Show accounts may need unshielding and just unshielded ones.
        _listItemsLiveData.postValue(allDoneAccounts.mapNotNull { account ->
            if (account.mayNeedUnshielding() || unshieldedAmountsByAccount.containsKey(account.address)) {
                // Show either the decrypted positive balance or the unshielded amount.
                val balance: BigInteger? =
                    if (account.encryptedBalanceStatus == ShieldedAccountEncryptionStatus.DECRYPTED
                        && account.totalShieldedBalance.signum() > 0
                    ) {
                        account.totalShieldedBalance
                    } else {
                        unshieldedAmountsByAccount[account.address]
                    }

                UnshieldingAccountListItem(
                    address = account.address,
                    name = account.getAccountName(),
                    balance = balance?.let { unshieldedAmount ->
                        getApplication<Application>().getString(
                            R.string.amount,
                            CurrencyUtil.formatGTU(unshieldedAmount)
                        )
                    },
                    isUnshielded = balance?.signum() == 0
                            || unshieldedAmountsByAccount.containsKey(account.address),
                )
            } else {
                null
            }
        })

        _waitingLiveData.postValue(false)
    }

    fun onUnshieldClicked(item: UnshieldingAccountListItem) = viewModelScope.launch {
        accountToUnshield = accountRepository.findByAddress(item.address)
            ?: error("Account to unshield $accountToUnshield not found")

        // Decrypt the balance if needed, otherwise immediately proceed with unshielding.
        if (accountToUnshield.encryptedBalanceStatus != ShieldedAccountEncryptionStatus.DECRYPTED) {
            _showAuthLiveData.postValue(Event(true))
        } else {
            _openUnshieldingLiveData.postValue(Event(accountToUnshield.address))
        }
    }

    fun onAuthenticated(password: String) = viewModelScope.launch(Dispatchers.IO) {
        decryptAndGoToUnshielding(accountToUnshield, password)
    }

    private suspend fun decryptAndGoToUnshielding(account: Account, password: String) {
        _waitingLiveData.postValue(true)

        val storageAccountDataEncrypted = account.encryptedAccountData
        if (TextUtils.isEmpty(storageAccountDataEncrypted)) {
            _errorLiveData.postValue(Event(R.string.app_error_general))
            _waitingLiveData.postValue(false)
            return
        }
        val decryptedJson = App.appCore.getCurrentAuthenticationManager()
            .decryptInBackground(password, storageAccountDataEncrypted)

        if (decryptedJson == null) {
            _errorLiveData.postValue(Event(R.string.app_error_encryption))
            _waitingLiveData.postValue(false)
            return
        }

        val encryptionKey =
            App.appCore.gson.fromJson(decryptedJson, StorageAccountData::class.java)
                .encryptionSecretKey

        accountUpdater.decryptEncryptedAmounts(
            key = encryptionKey,
            account = account,
        )
        accountUpdater.decryptAllUndecryptedAmounts(
            secretPrivateKey = encryptionKey,
        )

        // Go to unshielding once the balance is updated.
        accountUpdater.setUpdateListener(object : AccountUpdater.UpdateListener {
            override fun onError(stringRes: Int) {
                _errorLiveData.postValue(Event(stringRes))
                _waitingLiveData.postValue(false)
            }

            override fun onDone(totalBalances: TotalBalancesData) {
                // Update the list as the balances are updated.
                findAccountsMayNeedUnshielding()

                // If the shielded balance is empty,
                // immediately show the unshielding result.
                if (account.totalShieldedBalance.signum() > 0) {
                    _openUnshieldingLiveData.postValue(Event(account.address))
                } else {
                    onUnshieldingResult(
                        UnshieldingResult(
                            accountAddress = account.address,
                            unshieldedAmount = BigInteger.ZERO,
                        )
                    )
                }

                _waitingLiveData.postValue(false)
            }

            override fun onNewAccountFinalized(accountName: String) {}
        })
        accountUpdater.updateForAccount(account)
    }

    fun onUnshieldingResult(unshieldingResult: UnshieldingResult) {
        unshieldedAmountsByAccount[unshieldingResult.accountAddress] =
            unshieldingResult.unshieldedAmount
        findAccountsMayNeedUnshielding()
    }

    override fun onCleared() {
        super.onCleared()
        accountUpdater.dispose()
    }
}
