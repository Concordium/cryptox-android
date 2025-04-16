package com.concordium.wallet.ui.onramp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.backend.wert.WertRepository
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.ui.onramp.swipelux.SwipeluxSettingsHelper
import com.concordium.wallet.ui.onramp.wert.WertWidgetHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CcdOnrampSitesViewModel(application: Application) : AndroidViewModel(application) {
    private val ccdOnrampSiteRepository: CcdOnrampSiteRepository by lazy(::CcdOnrampSiteRepository)
    private val wertRepository: WertRepository by lazy(::WertRepository)

    private val _listItemsLiveData = MutableLiveData<List<CcdOnrampListItem>>()
    val listItemsLiveData: LiveData<List<CcdOnrampListItem>> = _listItemsLiveData

    private val _wertSite = MutableSharedFlow<Pair<CcdOnrampSite, Boolean>?>()
    val wertSite = _wertSite.asSharedFlow()

    private val _sessionLoading = MutableStateFlow(false)
    val sessionLoading = _sessionLoading.asStateFlow()

    private val _error = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val error = _error.asSharedFlow()

    lateinit var accountAddress: String
        private set

    init {
        postItems()
    }

    fun initialize(accountAddress: String) {
        this.accountAddress = accountAddress
    }

    private fun getWertSessionId(accountAddress: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _sessionLoading.value = true
            wertRepository.getWertSessionDetails(
                walletAddress = accountAddress,
                success = { response ->
                    val wertSite = ccdOnrampSiteRepository.getSiteByName("Wert")
                    viewModelScope.launch {
                        wertSite?.let {
                            _wertSite.emit(
                                Pair(
                                    it.copy(url = WertWidgetHelper.getWidgetLink(response.sessionId)),
                                    false
                                )
                            )
                        }
                    }
                    _sessionLoading.value = false
                },
                failure = { error ->
                    _error.tryEmit(BackendErrorHandler.getExceptionStringRes(error))
                    _sessionLoading.value = false
                }
            )
        }
    }

    fun onSiteClicked(site: CcdOnrampSite) = viewModelScope.launch {
        when {
            site.name == "Wert" -> getWertSessionId(accountAddress)

            site.name == "Swipelux" -> _wertSite.emit(
                Pair(
                    site.copy(url = SwipeluxSettingsHelper.getWidgetSettings(accountAddress)),
                    false
                )
            )

            site.type == CcdOnrampSite.Type.DEX -> _wertSite.emit(Pair(site, false))

            else -> _wertSite.emit(Pair(site, true))
        }
    }

    fun clearWertSession() {
        _wertSite.tryEmit(null)
        _error.tryEmit(-1)
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
