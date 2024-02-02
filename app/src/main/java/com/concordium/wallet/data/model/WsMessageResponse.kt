package com.concordium.wallet.data.model

import android.os.Parcelable
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.util.HexUtil.toHex
import com.concordium.wallet.util.HexUtil.toHexLE
import com.concordium.wallet.util.HexUtil.toHexLEx32
import com.concordium.wallet.util.decodeBase58ToHex
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.math.BigInteger

@Parcelize
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
) : Parcelable {

    companion object {
        const val MESSAGE_TYPE_ACCOUNT_INFO = "AccountInfo"
        const val MESSAGE_TYPE_SIMPLE_TRANSFER = "SimpleTransfer"
        const val MESSAGE_TYPE_TRANSACTION = "Transaction"
        const val MESSAGE_TYPE_SIGN_REQUEST = "SignRequest"
    }

    fun toJson(): String = Gson().toJson(this)

    @Parcelize
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
    ) : Parcelable {
        val receiveName: String?
            get() {
                return if (contractName != null && contractMethod != null)
                    "$contractName.$contractMethod"
                else
                    null
            }

        fun toJson(): String = Gson().toJson(this)

        @Parcelize
        data class ContractAddress(
            @SerializedName("address")
            val address: String,
            @SerializedName("index")
            val index: String,
            @SerializedName("sub_index")
            val subIndex: String
        ) : Parcelable

        @Parcelize
        data class Params(
            @SerializedName("param_name")
            val paramName: TypedName? = null,
            @SerializedName("param_type")
            val paramType: String? = null,
            @SerializedName("param_value")
            val paramValue: String? = null
        ) : Parcelable {
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

        fun getParams(): String? {
            return when (contractAddress?.index) {
                "93" -> getParamsSingle() // https://gitlab.com/dragonfly-bit/concordium-smart-contract/-/blob/master/v2/erc721/inventory/README.md
                "94" -> getParamsTraderFixedPriceWithFee721() // https://gitlab.com/dragonfly-bit/concordium-smart-contract/-/blob/master/v2/erc721/trader_fixed_price_with_fee/README.md
                "95" -> getParamsAuction() // https://gitlab.com/dragonfly-bit/concordium-smart-contract/-/blob/master/v2/erc721/trader_auction/README.md
                "96" -> getParamsMultiply() // https://gitlab.com/dragonfly-bit/concordium-smart-contract/-/blob/master/v2/erc1155/inventory/README.md
                "99", "110" -> getParamsAuctionMultiply() // https://gitlab.com/dragonfly-bit/concordium-smart-contract/-/blob/master/v2/erc1155/trader_auction/README.md
                "98", "109" -> getParamsTraderFixedPriceWithFee() // https://gitlab.com/dragonfly-bit/concordium-smart-contract/-/blob/master/v2/erc1155/trader_fixed_price_with_fee/README.md
                else -> ""
            }
        }

        private fun getParamsTraderFixedPriceWithFee(): String? {
            return when (contractName) {
                "trader" -> {
                    when (contractMethod) {
                        "create" -> {
                            "${getTokenId()}${getValue()}${getRoyalty()}${getUrl()}"
                        }
                        "create_and_sell" -> {
                            "${getTokenId()}${getValue()}${getLotId()}${getRoyalty()}${getUrl()}${getPrice()}"
                        }
                        "sell" -> {
                            "${getTokenId()}${getValue()}${getLotId()}${getPrice()}"
                        }
                        "buy", "pause", "close" -> {
                            getLotId()
                        }
                        else -> null
                    }
                }
                else -> null
            }
        }

        private fun getParamsTraderFixedPriceWithFee721(): String? {
            return when (contractName) {
                "trader" -> {
                    when (contractMethod) {
                        "create" -> {
                            "${getTokenId()}${getRoyalty()}${getUrl()}"
                        }
                        "create_and_sell" -> {
                            "${getTokenId()}${getRoyalty()}${getUrl()}${getPrice()}"
                        }
                        "sell" -> {
                            "${getTokenId()}${getPrice()}"
                        }
                        "buy", "pause", "close" -> {
                            getTokenId()
                        }
                        "clean_up" -> {
                            "${getTokenId()}${getSender()}"
                        }
                        else -> null
                    }
                }
                else -> null
            }
        }

        private fun getParamsAuctionMultiply(): String? {
            return when (contractName) {
                "inventory" -> {
                    when (contractMethod) {
                        "create" -> {
                            "${getTokenId()}${0.toHexLE()}${getRoyalty()}${getUrl()}"
                        }
                        else -> ""
                    }
                }
                "trader" -> {
                    when (contractMethod) {
                        "create" -> {
                            "${getTokenId()}${getValue()}${getRoyalty()}${getUrl()}"
                        }
                        "create_and_sell" -> {
                            "${getTokenId()}${getValue()}${getLotId()}${getRoyalty()}${getUrl()}${getPrice()}${getToTime()}${getBidAdditionalTime()}"
                        }
                        "sell" -> {
                            "${getTokenId()}${getValue()}${getLotId()}${getPrice()}${getToTime()}${getBidAdditionalTime()}"
                        }
                        "buy", "finalize", "pause", "close" -> {
                            getLotId()
                        }
                        "clean_up" -> {
                            ""
                        }
                        else -> ""
                    }
                }
                else -> {
                    null
                }
            }
        }

        private fun getParamsAuction(): String? {
            return when (contractName) {
                "inventory" -> {
                    when (contractMethod) {
                        "create" -> {
                            "${getTokenId()}${0.toHexLE()}${getRoyalty()}${getUrl()}"
                        }
                        else -> ""
                    }
                }
                "trader" -> {
                    when (contractMethod) {
                        "create" -> {
                            "${getTokenId()}${getRoyalty()}${getUrl()}"
                        }
                        "create_and_sell" -> {
                            "${getTokenId()}${getRoyalty()}${getUrl()}${getPrice()}${getToTime()}${getBidAdditionalTime()}"
                        }
                        "sell" -> {
                            "${getTokenId()}${getPrice()}${getToTime()}${getBidAdditionalTime()}"
                        }
                        "buy", "finalize", "pause", "close" -> {
                            getTokenId()
                        }
                        "clean_up" -> {
                            "${getTokenId()}${getSender()}"
                        }
                        else -> ""
                    }
                }
                else -> {
                    null
                }
            }
        }

        private fun getParamsMultiply(): String? {
            return when (contractName) {
                "inventory" -> {
                    when (contractMethod) {
                        "create" -> {
                            "${getTokenId()}${getValue()}${getCreator()}${getRoyalty()}${getUrl()}"
                        }
                        "transfer" -> {
                            "${getTokenId()}${getAddressFrom()}${getAddressTo()}"
                        }
                        "safe_transfer_from" -> {
                            "${getTokenId()}${getValue()}${getAddressFrom()}${getAddressTo()}"
                        }
                        "sell" -> {
                            "${getTokenId()}${getValue()}${getLotId()}${getSender()}00"
                        }
                        else -> ""
                    }
                }
                "trader" -> {
                    when (contractMethod) {
                        "create_and_sell" -> {
                            "${getTokenId()}${getRoyalty()}${getUrl()}${getPrice()}"
                        }
                        "sell" -> {
                            "${getTokenId()}${getPrice()}"
                        }
                        "buy" -> {
                            getTokenId()
                        }
                        else -> ""
                    }
                }
                else -> {
                    null
                }
            }
        }

        private fun getParamsSingle(): String? { // https://gitlab.com/dragonfly-bit/concordium-smart-contract/-/blob/master/v2/erc721/inventory/README.md
            return when (contractName) {
                "inventory" -> {
                    when (contractMethod) {
                        "create" -> {
                            "${getTokenId()}${0.toHexLE()}${getRoyalty()}${getUrl()}"
                        }
                        "transfer_from" -> {
                            "${getTokenId()}${getAddressFrom()}${getAddressTo()}"
                        }
                        "sell", "pause", "close" -> {
                            "${getTokenId()}${getSender()}"
                        }
                        else -> ""
                    }
                }
                "trader" -> {
                    when (contractMethod) {
                        "create_and_sell" -> {
                            "${getTokenId()}${getRoyalty()}${getUrl()}${getPrice()}"
                        }
                        "sell" -> {
                            "${getTokenId()}${getPrice()}"
                        }
                        "buy" -> {
                            getTokenId()
                        }
                        else -> ""
                    }
                }
                else -> {
                    null
                }
            }
        }

        private fun getUrl(): String {
            val url =
                contractParams?.find { it.paramName == Params.TypedName.URL }?.paramValue ?: ""
            val len = url.length.toHexLEx32()
            return len + url.toByteArray().toHex()
        }

        private fun getBidAdditionalTime(): String {
            return (contractParams?.find { it.paramName == Params.TypedName.BID_ADDITIONAL_TIME }?.paramValue?.toLong()
                ?: 0).toHexLE()
        }

        private fun getToTime(): String {
            return (contractParams?.find { it.paramName == Params.TypedName.TO_TIME }?.paramValue?.toLong()
                ?: 0).toHexLE()
        }

        private fun getTokenId(): String {
            return (contractParams?.find { it.paramName == Params.TypedName.TOKEN_ID }?.paramValue?.toLong()
                ?: 0).toHexLE()
        }

        private fun getLotId(): String {
            return (contractParams?.find { it.paramName == Params.TypedName.LOT_ID }?.paramValue?.toLong()
                ?: 0).toHexLE()
        }

        private fun getRoyalty(): String {
            val royaltyPercent =
                contractParams?.find { it.paramName == Params.TypedName.ROYALTY_PERCENT }?.paramValue
            val value =
                contractParams?.find { it.paramName == Params.TypedName.ROYALTY }?.paramValue
                    ?: royaltyPercent
            val ret: Long = try {
                value?.toLong() ?: 0
            } catch (ex: Exception) {
                0
            }
            return ret.toHexLE()
        }

        private fun getPrice(): String {
            val value = contractParams?.find { it.paramName == Params.TypedName.PRICE }?.paramValue
            val ret: Long = try {
                value?.toLong() ?: 0
            } catch (ex: Exception) {
                0
            }
            return ret.toHexLE()
        }

        private fun getAddressFrom(): String {
            return (contractParams?.find { it.paramName == Params.TypedName.ACCOUNTADDRESS_FROM }?.paramValue
                ?: "").decodeBase58ToHex()
        }

        private fun getSender(): String {
            return (contractParams?.find { it.paramName == Params.TypedName.SENDER }?.paramValue
                ?: "").decodeBase58ToHex()
        }

        private fun getCreator(): String {
            val addr = contractParams?.find { it.paramName == Params.TypedName.CREATOR }?.paramValue
            return if (addr.isNullOrEmpty()) {
                0.toHexLE()
            } else {
                addr.decodeBase58ToHex()
            }
        }

        private fun getValue(): String {
            val value = contractParams?.find { it.paramName == Params.TypedName.VALUE }?.paramValue
            val ret: Long = try {
                value?.toLong() ?: 0
            } catch (ex: Exception) {
                0
            }
            return ret.toHexLE()
        }

        private fun getAddressTo(): String {
            return (contractParams?.find { it.paramName == Params.TypedName.ACCOUNTADDRESS_TO }?.paramValue
                ?: "").decodeBase58ToHex()
        }
    }
}
