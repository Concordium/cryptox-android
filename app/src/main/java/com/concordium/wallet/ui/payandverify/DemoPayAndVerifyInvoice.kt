package com.concordium.wallet.ui.payandverify

import java.math.BigInteger
import java.time.Instant

data class DemoPayAndVerifyInvoice(
    val paymentDetails: PaymentDetails,
    val minAgeYears: Int,
    val proofRequestJson: String,
) {

    sealed interface PaymentDetails {

        data class Cis2(
            val amount: BigInteger,
            val tokenSymbol: String,
            val tokenDecimals: Int,
            val tokenContractIndex: Int,
            val tokenContractName: String,
            val recipientAccountAddress: String,
        ) : PaymentDetails
    }
}
