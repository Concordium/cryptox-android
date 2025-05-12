package com.concordium.wallet.ui.payandverify

import java.io.Serializable
import java.math.BigInteger

data class DemoPayAndVerifyInvoice(
    val storeName: String,
    val paymentDetails: PaymentDetails,
    val minAgeYears: Int,
    val proofRequestJson: String,
) : Serializable {

    sealed interface PaymentDetails : Serializable {

        data class Cis2(
            val amount: BigInteger,
            val tokenSymbol: String,
            val tokenDecimals: Int,
            val tokenId: String,
            val tokenContractIndex: Int,
            val tokenContractName: String,
            val recipientAccountAddress: String,
        ) : PaymentDetails
    }
}
