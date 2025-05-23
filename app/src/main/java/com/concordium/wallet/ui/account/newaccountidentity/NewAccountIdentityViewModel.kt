package com.concordium.wallet.ui.account.newaccountidentity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.util.Log

class NewAccountIdentityViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var accountName: String

    private val identityRepository = IdentityRepository(App.appCore.session.walletStorage.database.identityDao())
    val identityListLiveData = identityRepository.allDoneIdentities

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _errorDialogLiveData = MutableLiveData<Event<Int>>()
    val errorDialogLiveData: LiveData<Event<Int>>
        get() = _errorDialogLiveData

    fun initialize(accountName: String) {
        this.accountName = accountName
    }

    fun canCreateAccountForIdentity(identity: Identity): Boolean {
        val nextAccountNumber = identity.nextAccountNumber
        Log.d("nextAccountNumber: $nextAccountNumber")
        val maxAccounts = identity.identityObject!!.attributeList.maxAccounts
        if (nextAccountNumber >= maxAccounts) {
            _errorDialogLiveData.value =
                Event(R.string.new_account_identity_attributes_error_max_accounts)
            return false
        }
        return true
    }
}
