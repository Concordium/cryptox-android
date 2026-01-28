package com.concordium.wallet.data.util

import com.concordium.sdk.transactions.CCDAmount
import com.concordium.sdk.transactions.Parameter
import com.concordium.sdk.transactions.Payload
import com.concordium.sdk.transactions.ReceiveName
import com.concordium.sdk.transactions.TransactionHeader
import com.concordium.sdk.transactions.UpdateContract
import com.concordium.sdk.types.ContractAddress
import com.concordium.sdk.types.UInt64
import com.reown.util.hexToBytes

/**
 * Local transaction energy cost calculations.
 *
 * @see <a href="https://github.com/Concordium/concordium-rust-sdk/blob/01b00f7a82e62d3642be51282e6a89045727759d/src/types/transactions.rs#L1276">Rust SDK reference methods</a>
 * @see [Payload.calculateEnergyCost]
 */
object TransactionEnergyCostCalculator {

    /**
     * @param receiveName `contractName.methodName`
     * @param messageHex serialized parameters (payload) in hex
     * @param maxContractExecutionEnergy the corresponding value provided by a dApp
     * @param numSignatures number of signatures, which is 1 unless we implement multisig in the wallet.
     *
     * @return max energy (NRG) which can be spent by this transaction.
     */
    fun getContractTransactionMaxEnergy(
        receiveName: String,
        messageHex: String,
        maxContractExecutionEnergy: Long,
        numSignatures: Int = 1,
    ): Long = getContractTransactionMaxEnergy(
        payload = UpdateContract.from(
            CCDAmount.from(0L),
            ContractAddress(0L, 0L),
            ReceiveName.parse(receiveName),
            Parameter.from(messageHex.hexToBytes())
        ),
        maxContractExecutionEnergy = maxContractExecutionEnergy,
        numSignatures = numSignatures,
    )

    /**
     * @param payload contract transaction payload
     * @param maxContractExecutionEnergy the corresponding value provided by a dApp
     * @param numSignatures number of signatures, which is 1 unless we implement multisig in the wallet.
     *
     * @return max energy (NRG) which can be spent by this transaction.
     */
    private fun getContractTransactionMaxEnergy(
        payload: UpdateContract,
        maxContractExecutionEnergy: Long,
        numSignatures: Int = 1,
    ): Long = TransactionHeader.calculateMaxEnergyCost(
        numSignatures,
        payload.bytes.size,
        UInt64.from(maxContractExecutionEnergy),
    ).value
}
