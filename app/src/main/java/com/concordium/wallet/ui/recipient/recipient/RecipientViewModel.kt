package com.concordium.wallet.ui.recipient.recipient

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.room.Recipient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RecipientViewModel(application: Application) : AndroidViewModel(application) {

    private val cryptoLibrary = App.appCore.cryptoLibrary
    private val recipientRepository =
        RecipientRepository(App.appCore.session.walletStorage.database.recipientDao())
    lateinit var recipient: Recipient
    var editRecipientMode = false

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData
    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData
    private val _finishScreenLiveData = MutableLiveData<Event<Boolean>>()
    val finishScreenLiveData: LiveData<Event<Boolean>>
        get() = _finishScreenLiveData

    fun initialize(recipient: Recipient?) {
        if (recipient != null) {
            this.recipient = recipient
            editRecipientMode = true
        } else {
            this.recipient = Recipient(0, "", "")
        }
    }

    fun validateRecipient(name: String, address: String): Boolean {
        return (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(address))
    }

    fun validateAndSaveRecipient(name: String, address: String): Boolean {
        val isAddressValid = cryptoLibrary.checkAccountAddress(address)
        if (!isAddressValid) {
            _errorLiveData.value = Event(R.string.recipient_error_invalid_address)
            return false
        }

        val existingRecipient = runBlocking(Dispatchers.IO) {
            recipientRepository.getRecipientByAddress(address)
        }

        // We found an existing entry, which we should not as we are not in edit mode
        if (existingRecipient != null && !editRecipientMode) {
            _errorLiveData.value = Event(R.string.error_adding_account_duplicate)
            return false
        }

        // We found an existing entry, in edit mode, but it is not the same id - so we are creating a duplicate
        if (existingRecipient != null && editRecipientMode && existingRecipient.id != recipient.id) {
            _errorLiveData.value = Event(R.string.error_adding_account_duplicate)
            return false
        }

        recipient.name = name
        recipient.address = address
        _waitingLiveData.value = true
        saveRecipient(recipient)
        return true
    }

    private fun saveRecipient(recipient: Recipient) = viewModelScope.launch(Dispatchers.IO) {
        if (editRecipientMode) {
            recipientRepository.update(recipient)
        } else {
            recipientRepository.insert(recipient)
        }

        _finishScreenLiveData.postValue(Event(true))
    }
}
