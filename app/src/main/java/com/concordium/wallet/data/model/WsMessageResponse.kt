package com.concordium.wallet.data.model

import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.math.BigInteger

data class WsMessageResponse(
    @SerializedName("originator")
    val originator: String = "CryptoX Wallet Android app",
    @SerializedName("request_id")
    val requestId: String = "",
    @SerializedName("network_id")
    val networkId: String = "stage",
    var data: Payload? = null,
    @SerializedName("message_type")
    val messageType: String = "",
    @SerializedName("user_status")
    val userStatus: String = ""
) : Serializable {

    companion object {
        const val MESSAGE_TYPE_ACCOUNT_INFO = "AccountInfo"
        const val MESSAGE_TYPE_SIMPLE_TRANSFER = "SimpleTransfer"
        const val MESSAGE_TYPE_TRANSACTION = "Transaction"
        const val MESSAGE_TYPE_SIGN_REQUEST = "SignRequest"
    }

    fun toJson(): String = Gson().toJson(this)

    data class Payload(
        @SerializedName("from")
        val from: String,
        @SerializedName("to")
        val to: String?,
        @SerializedName("amount")
        val amount: BigInteger,
        @SerializedName("tx_hash")
        val TxHash: String?,
        @SerializedName("tx_status")
        val TxStatus: String?,
        @SerializedName("message")
        val message: String?,
        @SerializedName("signer_address")
        val signer_address: String?,
        @SerializedName("sign")
        val sign: String?,
        @SerializedName("expiry")
        val expiry: Long?,
        @SerializedName("contract_address")
        val contractAddress: ContractAddress? = null,
        @SerializedName("contract_name")
        val contractName: String? = null,
        @SerializedName("contract_title")
        val contractTitle: String? = null,
        @SerializedName("contract_method")
        val contractMethod: String? = null,
        @SerializedName("contract_params")
        val contractParams: List<Params>? = null,
        @SerializedName("serialized_params")
        val serializedParams: String? = null,
        @SerializedName("nrg_limit")
        val energyLimit: Long = 100501
    ) : Serializable {
        val receiveName: String?
            get() {
                return if (contractName != null && contractMethod != null)
                    "$contractName.$contractMethod"
                else
                    null
            }

        fun toJson(): String = Gson().toJson(this)

        data class ContractAddress(
            @SerializedName("address")
            val address: String,
            @SerializedName("index")
            val index: String,
            @SerializedName("sub_index")
            val subIndex: String
        ) : Serializable

        data class Params(
            @SerializedName("param_name")
            val paramName: TypedName? = null,
            @SerializedName("param_type")
            val paramType: String? = null,
            @SerializedName("param_value")
            val paramValue: String? = null
        ) : Serializable {
            enum class TypedName {
                @SerializedName("token_id")
                TOKEN_ID,

                @SerializedName("accountaddress_from")
                ACCOUNTADDRESS_FROM,

                @SerializedName("accountaddress_to")
                ACCOUNTADDRESS_TO,

                @SerializedName("royalties")
                ROYALTY,

                @SerializedName("royalty_percent")
                ROYALTY_PERCENT,

                @SerializedName("price")
                PRICE,

                @SerializedName("to_time")
                TO_TIME,

                @SerializedName("bid_additional_time")
                BID_ADDITIONAL_TIME,

                @SerializedName("url")
                URL,

                @SerializedName("value")
                VALUE,

                @SerializedName("creator")
                CREATOR,

                @SerializedName("lot_id")
                LOT_ID,

                @SerializedName("sender")
                SENDER
            }
        }

        fun getFormattedAmount() = CurrencyUtil.formatGTU(amount)

        fun getTitle(): Int? {
            return when ("$contractName.$contractMethod") {
                "trader.close" -> R.string.close_nft
                "inventory.close" -> R.string.close_nft
                "inventory.create" -> R.string.title_create_nft
                "inventory.transfer_from" -> R.string.title_transfer_nft
                "inventory.transfer" -> R.string.title_transfer_nft
                "inventory.safe_transfer_from" -> {
                    when (contractAddress?.index) {
                        "165" -> R.string.close_nft
                        else -> R.string.title_transfer_nft
                    }
                }

                "trader.create_and_sell" -> R.string.title_create_sell_nft
                "trader.sell" -> R.string.title_sell_nft
                "trader.buy" -> R.string.title_buy_nft
                "trader.pause" -> R.string.title_pause_nft
                "trader.clean_up" -> R.string.title_clean_up_nft
                "trader.finalize" -> R.string.title_finalize_nft
                else -> null
            }
        }
    }
}
