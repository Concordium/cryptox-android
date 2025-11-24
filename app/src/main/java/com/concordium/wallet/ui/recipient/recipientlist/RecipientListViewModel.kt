package com.concordium.wallet.ui.recipient.recipientlist

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipientListViewModel(application: Application) : AndroidViewModel(application) {

    private var senderAccount: Account? = null
    private var isShielded: Boolean = false
    private val recipientRepository =
        RecipientRepository(App.appCore.session.walletStorage.database.recipientDao())

    private val allRecipientsLiveData = recipientRepository.allRecipients

    private val _recipientsList = MutableStateFlow<List<RecipientListItem>>(listOf())
    val recipientsList = _recipientsList.asStateFlow()

    private val cryptoLibrary = App.appCore.cryptoLibrary

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    @SuppressLint("CheckResult")
    fun initialize(
        isShielded: Boolean,
        senderAccount: Account?,
    ) {
        this.isShielded = isShielded
        this.senderAccount = senderAccount

        allRecipientsLiveData.observeForever { allRecipients ->
            val filtered = if (senderAccount != null) {
                allRecipients.filter { it.address != senderAccount.address }
            } else {
                allRecipients
            }
            val items = filtered
                .map { it.toRecipientItem() }
                .groupBy { it.recipientType }
                .flatMap { (recipientType, recipients) ->
                    listOf(RecipientListItem.Category(recipientType)) + recipients
                }
            _recipientsList.value = items
        }
    }

    fun deleteRecipient(recipient: Recipient) = viewModelScope.launch(Dispatchers.IO) {
        recipientRepository.delete(recipient)
    }

    fun canGoBackWithRecipientAddress(address: String): Boolean {
        return senderAccount != null && cryptoLibrary.checkAccountAddress(address)
    }
}
