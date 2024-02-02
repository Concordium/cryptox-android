package com.concordium.wallet.ui.tokens.provider

import android.app.Application
import android.os.Parcelable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.backend.tokens.TokensRepository
import com.concordium.wallet.data.model.WalletMeta
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.util.Event
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Token(
    @SerializedName("blockchain_id")
    val blockchainId: String,
    @SerializedName("smartcontract_address")
    val smartContractAddress: List<String>,
    @SerializedName("nft_id")
    val nftId: String,
    @SerializedName("marketplace")
    val marketplace: String,
    @SerializedName("marketplace_name")
    val marketplaceName: String,
    @SerializedName("nft_page")
    val nftPage: String,
    @SerializedName("nft_metadata_url")
    val nftMetadataUrl: String,
    @SerializedName("nft_name")
    val nftName: String,
    @SerializedName("owner_address")
    val ownerAddress: String,
    @SerializedName("author_address")
    val authorAddress: String,
    @SerializedName("author_royalty")
    val authorRoyalty: String,
    @SerializedName("icon_preview_url")
    val iconPreviewUrl: String,
    @SerializedName("image_url")
    val imageUrl: String,
    @SerializedName("quantity")
    val quantity: String,
    @SerializedName("total_minted")
    val totalMinted: String,
    @SerializedName("sale_status")
    val saleStatus: String
)

@Parcelize
data class ProviderMeta(
    @SerializedName("name")
    val name: String,
    @SerializedName("website")
    val website: String,
    @SerializedName("system")
    val system: Boolean = false,
    @SerializedName("wallets")
    var wallets: List<WalletMeta> = emptyList()
) : Parcelable {

    @Transient
    var isShowMenu = false
}

class ProvidersOverviewViewModel(application: Application) : AndroidViewModel(application) {

    private val tokensRepo = TokensRepository()

    private val _conversationListLiveData: MutableLiveData<Event<List<Token>>> = MutableLiveData()
    fun tokensList(): LiveData<Event<List<Token>>> = _conversationListLiveData

    private val _onAccountReadyLiveData: MutableLiveData<Event<List<WalletMeta>>> = MutableLiveData()
    fun onAccountReady(): LiveData<Event<List<WalletMeta>>> = _onAccountReadyLiveData

    private val _onProvidersReadyLiveData: MutableLiveData<Event<List<ProviderMeta>>> = MutableLiveData()
    fun onProvidersReady(): LiveData<Event<List<ProviderMeta>>> = _onProvidersReadyLiveData

    private val providers = mutableListOf<ProviderMeta>()
    private val accounts = mutableListOf<WalletMeta>()

    fun processAction(action: ProvidersViewAction) {
        when (action) {
            ProvidersViewAction.GetAccount -> getAccount()
            is ProvidersViewAction.GetProviders -> getProviders(action.providers)
            else -> {}
        }
    }

    private fun deleteProvider(providerMeta: ProviderMeta) {
    }

    private fun getProviders(providers: List<ProviderMeta>) {
        this.providers.clear()
        this.providers.add(ProviderMeta(name = "Spaceseven", website = BuildConfig.S7_DOMAIN, system = true))
        this.providers.addAll(providers)
        processProviders()
    }

    private fun processProviders() {
        viewModelScope.launch(Dispatchers.IO) {
            providers.forEach { provider ->
                val wallets = mutableListOf<WalletMeta>()
                accounts.forEach { wallet ->
                    val toks = tokensRepo.getTokens(provider.website, wallet.address)
                    if (toks != null) {
                        val w = wallet.copy(total = toks.total)
                        wallets.add(w)
                    }
                }
                provider.wallets = wallets
            }

            _onProvidersReadyLiveData.postValue(Event(providers))
        }
    }

    private fun getAccount() = viewModelScope.launch(viewModelScope.coroutineContext) {
        val accountDao = WalletDatabase.getDatabase(getApplication()).accountDao()
        val accountRepository = AccountRepository(accountDao)
        val acc = withContext(Dispatchers.IO) {
            accountRepository.getAll()
        }
        accounts.clear()
        accounts.addAll(acc.map { WalletMeta(it.name, it.address, 0) })

        // "4oojyKb9C9K5EG7YH3wxcnC9YoW7MupJrt1xsscfH75HbnAhnm", "4mf4sgYSrfvakr3fm41X2gduGtdfQnxoSnPDb8nHPwn75V9WHF"
//        accounts.add(WalletMeta("Wall 1", "4oojyKb9C9K5EG7YH3wxcnC9YoW7MupJrt1xsscfH75HbnAhnm", 0))
//        accounts.add(WalletMeta("Wall 2", "4mf4sgYSrfvakr3fm41X2gduGtdfQnxoSnPDb8nHPwn75V9WHF", 0))

        _onAccountReadyLiveData.postValue(Event(accounts.toList()))
    }

    private fun getMarketplaces(): List<String> {
        return listOf(BuildConfig.S7_DOMAIN)
    }
}