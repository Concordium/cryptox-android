package com.concordium.wallet.ui.onramp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class CcdOnrampSitesViewModel(application: Application) : AndroidViewModel(application) {
    private val ccdOnrampSiteRepository: CcdOnrampSiteRepository by lazy(::CcdOnrampSiteRepository)

    private val _listItemsLiveData = MutableLiveData<List<CcdOnrampListItem>>()
    val listItemsLiveData: LiveData<List<CcdOnrampListItem>> = _listItemsLiveData

    init {
        postItems()
    }

    private fun postItems() {
        val sites = ccdOnrampSiteRepository.getSites()
        val items = mutableListOf<CcdOnrampListItem>()

        items.add(CcdOnrampListItem.Header)
        sites
            .groupBy(CcdOnrampSite::type)
            .forEach { (type, sites) ->
                items.add(CcdOnrampListItem.Section(type))
                items.addAll(
                    sites.mapIndexed { i, site ->
                        CcdOnrampListItem.Site(
                            source = site,
                            isDividerVisible = i < sites.size - 1,
                        )
                    })
            }
        items.add(CcdOnrampListItem.Disclaimer)

        _listItemsLiveData.postValue(items)
    }
}
