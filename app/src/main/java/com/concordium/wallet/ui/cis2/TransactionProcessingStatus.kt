package com.concordium.wallet.ui.cis2

sealed interface TransactionProcessingStatus {

    object Loading : TransactionProcessingStatus

    object Success : TransactionProcessingStatus

    class Fail(
        val errorRes: Int,
    ) : TransactionProcessingStatus
}
