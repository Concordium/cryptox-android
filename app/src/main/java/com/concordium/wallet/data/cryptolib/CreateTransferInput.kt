package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.InputEncryptedAmount
import java.io.Serializable

data class CreateTransferInput(
    val from: String,
    val keys: AccountData,
    val to: String?,
    val expiry: Long,
    val amount: String?,
    val energy: Long,
    val nonce: Int,
    val memo: String?,
    val global: GlobalParams?,
    val receiverPublicKey: String?,
    val senderSecretKey: String?,
    val inputEncryptedAmount: InputEncryptedAmount?,
): Serializable
