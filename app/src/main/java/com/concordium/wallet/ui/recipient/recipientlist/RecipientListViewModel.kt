package com.concordium.wallet.ui.recipient.recipientlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecipientListViewModel(application: Application) : AndroidViewModel(application) {

    private var senderAccount: Account? = null
    private var isShielded: Boolean = false
    private val recipientRepository =
        RecipientRepository(App.appCore.session.walletStorage.database.recipientDao())

    private val allRecipientsLiveData = recipientRepository.allRecipients
    val recipientListLiveData: LiveData<List<Recipient>>
        get() = allRecipientsLiveData.switchMap { allRecipients ->
            val filteredRecipientsLiveData = MutableLiveData<List<Recipient>>()
            val senderAccount = this.senderAccount
            val recipientsToShowLiveData = when {
                senderAccount != null -> {
                    val filteredList = allRecipients.filter { recipient ->
                        recipient.address != senderAccount.address
                    }
                    filteredRecipientsLiveData.value = filteredList
                    filteredRecipientsLiveData
                }

                else -> {
                    filteredRecipientsLiveData.value = allRecipients
                    filteredRecipientsLiveData
                }
            }
            recipientsToShowLiveData
        }
    private val cryptoLibrary = App.appCore.cryptoLibrary

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    fun initialize(
        isShielded: Boolean,
        senderAccount: Account?,
    ) {
        this.isShielded = isShielded
        this.senderAccount = senderAccount
    }

    fun deleteRecipient(recipient: Recipient) = viewModelScope.launch(Dispatchers.IO) {
        recipientRepository.delete(recipient)
    }

    fun canGoBackWithRecipientAddress(address: String): Boolean {
        return senderAccount != null && cryptoLibrary.checkAccountAddress(address)
    }
}
