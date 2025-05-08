package com.concordium.wallet.ui.payandverify

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface DemoPayAndVerifyInvoiceBackend {

    @GET
    suspend fun getInvoice(
        @Url
        invoiceUrl: String,
    ): InvoiceResponse

    @POST("pay")
    suspend fun payInvoice(
        @Body
        request: PaymentRequest,
    )

    class InvoiceResponse(
        val version: Int = 1,
        val id: String,
        val status: String,
        val minAgeYears: Int,
        val proofRequestJson: String,
        val paymentType: String,
        val cis2Amount: String?,
        val cis2TokenContractIndex: Int?,
        val cis2TokenContractName: String?,
        val cis2TokenSymbol: String?,
        val cis2TokenDecimals: Int?,
        val cis2RecipientAccountAddress: String?,
    )

    class PaymentRequest(
        val proofJson: String,
        val paymentTransactionHex: String,
    )
}
