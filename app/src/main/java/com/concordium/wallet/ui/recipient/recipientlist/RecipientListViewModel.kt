package com.concordium.wallet.ui.recipient.recipientlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.RecentRecipientRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class RecipientListViewModel(application: Application) : AndroidViewModel(application) {

    private var senderAccount: Account? = null
    private var isShielded: Boolean = false
    private val recipientRepository =
        RecipientRepository(App.appCore.session.walletStorage.database.recipientDao())
    private val recentRecipientRepository =
        RecentRecipientRepository(App.appCore.session.walletStorage.database.recentRecipientDao())

    private val allRecipientsFlow = recipientRepository.allRecipients
    private val allRecentRecipientsFlow = recentRecipientRepository.allRecentRecipients

    private val _recipientsList = MutableStateFlow<List<RecipientListItem>>(listOf())

    private val _filteredList = MutableStateFlow<List<RecipientListItem>>(listOf())
    val filteredList = _filteredList.asStateFlow()

    private val _waiting = MutableStateFlow(true)
    val waiting = _waiting.asStateFlow()

    private val _selectedRecipient = MutableStateFlow<Recipient?>(null)

    private val cryptoLibrary = App.appCore.cryptoLibrary

    fun initialize(
        isShielded: Boolean,
        senderAccount: Account?,
    ) {
        this.isShielded = isShielded
        this.senderAccount = senderAccount

        initRecipientsList(senderAccount)
    }

    fun deleteRecipient() = viewModelScope.launch(Dispatchers.IO) {
        recipientRepository.delete(_selectedRecipient.value?.address ?: "")
    }

    fun setRecipient(recipient: Recipient?) {
        _selectedRecipient.tryEmit(recipient)
    }

    fun canGoBackWithRecipientAddress(address: String): Boolean {
        return senderAccount != null && cryptoLibrary.checkAccountAddress(address)
    }

    private fun initRecipientsList(senderAccount: Account?) = viewModelScope.launch {
        combine(
            allRecipientsFlow,
            allRecentRecipientsFlow
        ) { allRecipients, allRecentRecipients ->
            val filteredRecentRecipients =
                if (senderAccount != null) {
                    allRecentRecipients
                        .filter { it.address != senderAccount.address }
                        .take(2)
                } else emptyList()

            val filteredRecipients = if (senderAccount != null) {
                allRecipients.filter { it.address != senderAccount.address }
            } else allRecipients

            val items = (
                    filteredRecentRecipients.map { it.toRecipientItem() } +
                            filteredRecipients.map { it.toRecipientItem() }
                    )
                .groupBy { it.recipientType }
                .flatMap { (recipientType, recipients) ->
                    listOf(RecipientListItem.Category(recipientType)) + recipients
                }
            items
        }.collect { items ->
            _recipientsList.value = items
            _filteredList.value = items
            _waiting.value = false
        }
    }

    fun filter(filterString: String?) {
        val currentFilter = filterString ?: ""
        val allItems = _recipientsList.value
        if (currentFilter.isEmpty()) {
            _filteredList.value = allItems
        } else {
            _filteredList.value = getFilteredList(
                _recipientsList.value,
                currentFilter
            )
        }
    }

    private fun getFilteredList(
        allData: List<RecipientListItem>,
        filterString: String
    ): List<RecipientListItem> {
        return allData
            .filterIsInstance<RecipientListItem.RecipientItem>()
            .filter { recipient ->
                recipient.name.lowercase().contains(filterString.lowercase()) ||
                        recipient.address.lowercase().contains(filterString.lowercase())
            }
            .distinctBy { it.address }
    }
}
