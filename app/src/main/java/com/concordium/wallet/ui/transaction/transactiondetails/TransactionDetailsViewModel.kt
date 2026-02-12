package com.concordium.wallet.ui.transaction.transactiondetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.App
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.SubmissionStatusResponse
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionSource
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.common.BackendErrorHandler

class TransactionDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val proxyRepository = ProxyRepository()

    private var transferSubmissionStatusRequest: BackendRequest<SubmissionStatusResponse>? = null
    lateinit var account: Account
    lateinit var transaction: Transaction

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _showDetailsLiveData = MutableLiveData<Event<Boolean>>()
    val showDetailsLiveData: LiveData<Event<Boolean>>
        get() = _showDetailsLiveData

    val canOpenExplorer: Boolean
        get() = App.appCore.session.isOpeningExplorerPossible()

    fun initialize(account: Account, transaction: Transaction) {
        this.account = account
        this.transaction = transaction
    }

    override fun onCleared() {
        super.onCleared()
        transferSubmissionStatusRequest?.dispose()
    }

    fun showData() {
        if (transaction.source == TransactionSource.Local) {
            // Update the local transaction before showing it
            transaction.hash?.let {
                _waitingLiveData.value = true
                loadTransferSubmissionStatus(it)
                return
            }
            _waitingLiveData.value = false
        } else {
            _waitingLiveData.value = false
            _showDetailsLiveData.value = Event(true)
        }
    }

    private fun loadTransferSubmissionStatus(submissionId: String) {
        _waitingLiveData.value = true
        transferSubmissionStatusRequest?.dispose()
        transferSubmissionStatusRequest = proxyRepository.getSubmissionStatus(
            submissionId,
            {
                // Update the transaction - the changes are not saved (they will be updated elsewhere)
                transaction.blockHashes = it.blockHashes
                transaction.status = it.status
                transaction.outcome = it.outcome ?: TransactionOutcome.UNKNOWN
                transaction.rejectReason = it.rejectReason
                _waitingLiveData.value = false
                _showDetailsLiveData.value = Event(true)
            },
            {
                _waitingLiveData.value = false
                _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(it))
            }
        )
    }

    fun getExplorerUrl(): String =
        App.appCore.session.network.ccdScanFrontendUrl!!
            .newBuilder()
            .addQueryParameter("dcount", "1")
            .addQueryParameter("dentity", "transaction")
            .addQueryParameter("dhash", transaction.hash)
            .toString()
}
