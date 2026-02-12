package com.concordium.wallet.ui.tokens.provider

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.backend.tokens.TokensRepository
import com.concordium.wallet.data.model.WalletMeta
import com.concordium.wallet.util.Event
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable

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
    val saleStatus: String,
)

data class ProviderMeta(
    @SerializedName("name")
    val name: String,
    @SerializedName("website")
    val website: String,
    @SerializedName("system")
    val system: Boolean = false,
    @SerializedName("wallets")
    var wallets: List<WalletMeta> = emptyList(),
) : Serializable {

    @Transient
    var isShowMenu = false
}

class ProvidersOverviewViewModel(application: Application) : AndroidViewModel(application) {

    private val tokensRepo = TokensRepository()

    private val _onAccountReadyLiveData: MutableLiveData<Event<List<WalletMeta>>> =
        MutableLiveData()

    fun onAccountReady(): LiveData<Event<List<WalletMeta>>> = _onAccountReadyLiveData

    private val _onProvidersReadyLiveData: MutableLiveData<Event<List<ProviderMeta>>> =
        MutableLiveData()

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

    private fun getProviders(providers: List<ProviderMeta>) {
        this.providers.clear()
        this.providers.add(
            ProviderMeta(
                name = "Spaceseven",
                website = App.appCore.session.network.spacesevenUrl!!.toString(),
                system = true,
            )
        )
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
        val accountRepository =
            AccountRepository(App.appCore.session.walletStorage.database.accountDao())
        val acc = withContext(Dispatchers.IO) {
            accountRepository.getAll()
        }
        accounts.clear()
        accounts.addAll(acc.map { WalletMeta(it.name, it.address, 0) })

        _onAccountReadyLiveData.postValue(Event(accounts.toList()))
    }
}
