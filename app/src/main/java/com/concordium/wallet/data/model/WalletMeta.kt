package com.concordium.wallet.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class WalletMeta(
    @SerializedName("name")
    val name: String,
    @SerializedName("address")
    val address: String,
    @SerializedName("total")
    var total: Int,
    @SerializedName("website")
    val website: String? = null
) : Parcelable