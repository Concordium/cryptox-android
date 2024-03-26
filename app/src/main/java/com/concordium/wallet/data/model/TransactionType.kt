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

    @SerializedName("blockReward")
    BLOCKREWARD,

    @SerializedName("finalizationReward")
    FINALIZATIONREWARD,

    @SerializedName("bakingReward")
    BAKINGREWARD,

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

    @SerializedName("initContract", alternate = ["InitContract"])
    INIT_CONTRACT,

    @SerializedName("deployModule")
    DEPLOY_MODULE,

    // This has been added to have a default value
    @SerializedName("unknown")
    UNKNOWN,
    ;
}

