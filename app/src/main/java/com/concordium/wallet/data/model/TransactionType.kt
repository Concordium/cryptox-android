package com.concordium.wallet.data.model

import com.google.gson.annotations.SerializedName

enum class TransactionType {
    @SerializedName("transfer", alternate = ["Transfer"])
    TRANSFER,

    @SerializedName("encryptedAmountTransfer")
    ENCRYPTEDAMOUNTTRANSFER,

    @SerializedName("transferToEncrypted")
    TRANSFERTOENCRYPTED,

    @SerializedName("transferToPublic")
    TRANSFERTOPUBLIC,

    @SerializedName("transferWithMemo")
    TRANSFERWITHMEMO,

    @SerializedName("encryptedAmountTransferWithMemo")
    ENCRYPTEDAMOUNTTRANSFERWITHMEMO,

    @SerializedName("update", alternate = ["Update"])
    UPDATE,

    @SerializedName("delegation")
    LOCAL_DELEGATION,

    @SerializedName("baker")
    LOCAL_BAKER,

    @SerializedName("validatorPrimedForSuspension")
    VALIDATOR_PRIMED_FOR_SUSPENSION,

    @SerializedName("validatorSuspended")
    VALIDATOR_SUSPENDED,

    @SerializedName("tokenUpdate")
    TOKEN_UPDATE,

    ;
}

