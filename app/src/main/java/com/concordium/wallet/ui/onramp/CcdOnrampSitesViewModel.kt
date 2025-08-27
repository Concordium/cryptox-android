package com.concordium.wallet.ui.onramp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.backend.wert.WertRepository
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.ui.onramp.banxa.BanxaWidgetHelper
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

    private val _siteToOpen = MutableSharedFlow<Pair<CcdOnrampSite, Boolean>?>()
    val siteToOpen = _siteToOpen.asSharedFlow()

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

    private fun openWert(
        accountAddress: String,
        site: CcdOnrampSite,
    ) = viewModelScope.launch(Dispatchers.IO) {
        _sessionLoading.value = true
        try {
            val sessionDetails = wertRepository.getWertSessionDetails(accountAddress)
            _siteToOpen.emit(
                Pair(
                    site.copy(url = WertWidgetHelper.getWidgetLink(sessionDetails.sessionId)),
                    false
                )
            )
        } catch (e: Exception) {
            _error.emit(BackendErrorHandler.getExceptionStringRes(e))
        } finally {
            _sessionLoading.value = false
        }
    }

    fun onSiteClicked(site: CcdOnrampSite) = viewModelScope.launch {
        when {
            site.name == "Wert" -> openWert(accountAddress, site)

            site.name == "Swipelux" -> _siteToOpen.emit(
                Pair(
                    site.copy(url = SwipeluxSettingsHelper.getWidgetSettings(accountAddress)),
                    false
                )
            )

            site.name.startsWith("Banxa") -> _siteToOpen.emit(
                Pair(
                    site.copy(
                        url = BanxaWidgetHelper.getWidgetLink(
                            baseUrl = site.url,
                            accountAddress = accountAddress,
                        )
                    ),
                    false
                )
            )

            else -> _siteToOpen.emit(Pair(site, true))
        }
    }

    fun isHasAcceptedOnRampDisclaimer() = App.appCore.setup.isHasAcceptedOnRampDisclaimer

    fun setHasAcceptedOnRampDisclaimer(isAccepted: Boolean) {
        App.appCore.setup.setHasAcceptedOnRampDisclaimer(isAccepted)
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
                    items.addAll(sites.map(CcdOnrampListItem::Site))
                }
        } else {
            items.add(CcdOnrampListItem.NoneAvailable)
        }

        items.add(CcdOnrampListItem.ExchangesNotice)

        _listItemsLiveData.postValue(items)
    }
}
