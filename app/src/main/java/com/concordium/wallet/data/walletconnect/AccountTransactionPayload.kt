package com.concordium.wallet.data.walletconnect

import com.concordium.sdk.responses.modulelist.ModuleRef
import com.concordium.sdk.responses.transactionstatus.DelegationTarget
import com.concordium.sdk.transactions.CCDAmount
import com.concordium.sdk.transactions.InitName
import com.concordium.sdk.transactions.Parameter
import com.concordium.sdk.transactions.tokens.TransferTokenOperation
import com.concordium.sdk.types.ContractAddress
import com.concordium.sdk.types.UInt64
import com.google.gson.annotations.SerializedName
import com.reown.util.hexToBytes
import java.math.BigInteger

sealed interface AccountTransactionPayload {

    data class Update(
        val address: ContractAddress,
        val amount: BigInteger,
        /**
         * Energy for the whole transaction including the administrative fee.
         * Legacy field.
         */
        val maxEnergy: Long?,
        /**
         * Energy for the smart contract execution only,
         * without the administrative transaction fee.
         */
        val maxContractExecutionEnergy: Long?,
        val message: String,
        val receiveName: String
    ) : AccountTransactionPayload

    data class Transfer(
        val amount: BigInteger,
        @SerializedName("to", alternate = ["toAddress"])
        val toAddress: String
    ) : AccountTransactionPayload

    data class PltTransfer(
        val tokenId: String,
        val transfer: TransferTokenOperation,
    ) : AccountTransactionPayload

    data class ConfigureDelegation(
        val amount: BigInteger,
        val restakeEarnings: Boolean,
        val delegationTarget: DelegationTarget?
    ) : AccountTransactionPayload

    data class InitContract private constructor(
        val initName: String,
        val amount: BigInteger,
        val maxContractExecutionEnergy: UInt64,
        val moduleRef: String,
        val param: String,
        val payload: com.concordium.sdk.transactions.InitContract,
    ) : AccountTransactionPayload {
        companion object {
            /**
             * Parse a WalletConnect InitContract DTO into SDK types.
             *
             * @param initName contract name, with or without the `init_` prefix
             * @param amount attached amount in microCCD
             * @param maxContractExecutionEnergy energy available for contract execution
             * @param moduleRef module reference, optionally prefixed by its serialized length
             * @param param serialized contract parameter in hexadecimal format
             * @return parsed payload ready for energy calculation and transaction construction
             * @throws IllegalArgumentException when the SDK rejects a field
             */
            fun parse(
                initName: String,
                amount: BigInteger,
                maxContractExecutionEnergy: Long,
                moduleRef: String,
                param: String,
            ): InitContract {
                val normalizedModuleRef =
                    if (moduleRef.length == 72 && moduleRef.startsWith("00000020")) {
                        moduleRef.substring(8)
                    } else {
                        moduleRef
                    }
                val normalizedInitName =
                    if (initName.startsWith("init_")) initName else "init_$initName"
                val payload = com.concordium.sdk.transactions.InitContract.from(
                    CCDAmount.fromMicro(amount.toString()),
                    ModuleRef.from(normalizedModuleRef),
                    InitName.from(normalizedInitName),
                    Parameter.from(param.hexToBytes()),
                )

                return InitContract(
                    initName = normalizedInitName,
                    amount = amount,
                    maxContractExecutionEnergy = UInt64.from(maxContractExecutionEnergy),
                    moduleRef = normalizedModuleRef,
                    param = param,
                    payload = payload,
                )
            }
        }
    }
}
