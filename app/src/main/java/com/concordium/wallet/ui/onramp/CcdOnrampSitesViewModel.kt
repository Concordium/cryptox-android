package com.concordium.wallet.ui.onramp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.room.WalletDatabase
import kotlinx.coroutines.launch

class CcdOnrampSitesViewModel(application: Application) : AndroidViewModel(application) {
    private val ccdOnrampSiteRepository: CcdOnrampSiteRepository by lazy(::CcdOnrampSiteRepository)
    private val accountRepository: AccountRepository by lazy {
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        AccountRepository(accountDao)
    }

    private val _listItemsLiveData = MutableLiveData<List<CcdOnrampListItem>>()
    val listItemsLiveData: LiveData<List<CcdOnrampListItem>> = _listItemsLiveData

    var accountAddress: String? = null
        private set

    init {
        postItems()
    }

    fun initialize(accountAddress: String?) {
        // Use the provided account address or, if none provided,
        // the only one if there is one account.
        if (accountAddress != null) {
            this.accountAddress = accountAddress
        } else {
            viewModelScope.launch {
                val allDoneAccounts = accountRepository.getAllDone()
                if (allDoneAccounts.size == 1) {
                    this@CcdOnrampSitesViewModel.accountAddress = allDoneAccounts.first().address
                }
            }
        }
    }

    private fun postItems() {
        val sites = ccdOnrampSiteRepository.getSites()
        val items = mutableListOf<CcdOnrampListItem>()

        items.add(CcdOnrampListItem.Header)

        if (sites.isNotEmpty()) {
            sites
                .groupBy(CcdOnrampSite::type)
                .forEach { (type, sites) ->
                    items.add(CcdOnrampListItem.Section(type))
                    items.addAll(
                        sites.map { site ->
                            CcdOnrampListItem.Site(
                                source = site,
                                isDividerVisible = false,
                            )
                        })
                }
        } else {
            items.add(CcdOnrampListItem.NoneAvailable)
        }

        items.add(CcdOnrampListItem.Disclaimer)

        _listItemsLiveData.postValue(items)
    }
}
