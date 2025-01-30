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

    var account: Account? = null
    var isShielded: Boolean = false
    private val recipientRepository =
        RecipientRepository(App.appCore.session.walletStorage.database.recipientDao())
    var selectRecipientMode = false
        private set

    private val allRecipientsLiveData = recipientRepository.allRecipients
    val recipientListLiveData: LiveData<List<Recipient>>
        get() = allRecipientsLiveData.switchMap { allRecipients ->
            val filteredRecipientsLiveData = MutableLiveData<List<Recipient>>()
            val recipientsToShowLiveData = when {
                selectRecipientMode -> {
                    val filteredList = allRecipients.filter { recipient ->
                        recipient.address != account?.address
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
        selectRecipientMode: Boolean,
        shielded: Boolean,
        account: Account?
    ) {
        this.selectRecipientMode = selectRecipientMode
        this.isShielded = shielded
        this.account = account
    }

    fun deleteRecipient(recipient: Recipient) = viewModelScope.launch(Dispatchers.IO) {
        recipientRepository.delete(recipient)
    }

    fun validateRecipientAddress(address: String): Boolean {
        return cryptoLibrary.checkAccountAddress(address)
    }
}
