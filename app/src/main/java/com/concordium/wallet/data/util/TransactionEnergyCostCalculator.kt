package com.concordium.wallet.data.util

import com.concordium.sdk.responses.modulelist.ModuleRef
import com.concordium.sdk.transactions.CCDAmount
import com.concordium.sdk.transactions.InitContract
import com.concordium.sdk.transactions.InitName
import com.concordium.sdk.transactions.Parameter
import com.concordium.sdk.transactions.Payload
import com.concordium.sdk.transactions.ReceiveName
import com.concordium.sdk.transactions.TransactionHeader
import com.concordium.sdk.transactions.UpdateContract
import com.concordium.sdk.types.ContractAddress
import com.concordium.sdk.types.UInt64
import com.reown.util.hexToBytes
import java.math.BigInteger

/**
 * Local transaction energy cost calculations.
 *
 * @see <a href="https://github.com/Concordium/concordium-rust-sdk/blob/01b00f7a82e62d3642be51282e6a89045727759d/src/types/transactions.rs#L1276">Rust SDK reference methods</a>
 * @see [Payload.calculateEnergyCost]
 */
object TransactionEnergyCostCalculator {

    private val MAX_UINT64 =
        BigInteger("18446744073709551615")

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
            Parameter.from(messageHex.hexToBytes()),
        ),
        maxContractExecutionEnergy = maxContractExecutionEnergy,
        numSignatures = numSignatures,
    )

    /**
     * Calculate max energy for a smart contract initialization transaction.
     *
     * @param initName smart contract init name
     * @param amount amount of CCD attached to the initialization, in microCCD
     * @param moduleRef module reference in hex format
     * @param paramHex serialized initialization parameters in hex
     * @param maxContractExecutionEnergy max energy available for contract execution
     * @param numSignatures number of signatures, which is 1 unless we implement multisig in the wallet.
     *
     * @return max energy (NRG) which can be spent by this transaction.
     */
    fun getContractInitMaxEnergy(
        initName: String,
        amount: BigInteger,
        moduleRef: String,
        paramHex: String,
        maxContractExecutionEnergy: Long,
        numSignatures: Int = 1,
    ): Long = getContractTransactionMaxEnergy(
        payload = InitContract.from(
            CCDAmount.fromMicro(
                validateInitAmount(amount).toString(),
            ),
            ModuleRef.from(normalizeModuleRef(moduleRef)),
            InitName.from(normalizeInitName(initName)),
            Parameter.from(
                validateInitParamHex(paramHex).hexToBytes(),
            ),
        ),
        maxContractExecutionEnergy =
            validateInitMaxContractExecutionEnergy(
                maxContractExecutionEnergy,
            ),
        numSignatures = numSignatures,
    )

    /**
     * Normalize and validate a smart contract init name.
     *
     * WalletConnect InitContract payloads may provide the contract name without
     * the `init_` prefix. The name is normalized and then validated by the
     * Concordium SDK using [InitName].
     *
     * @param initName smart contract init name, with or without the `init_` prefix
     *
     * @return validated init entrypoint name with the `init_` prefix
     *
     * @throws IllegalArgumentException if the resulting init name is invalid
     */
    fun normalizeInitName(initName: String): String {
        require(initName.isNotEmpty()) {
            "InitContract initName must not be empty"
        }

        val normalized =
            if (initName.startsWith("init_")) {
                initName
            } else {
                "init_$initName"
            }

        // Use the SDK as the canonical validator instead of duplicating
        // protocol-specific validation rules here.
        InitName.from(normalized)

        return normalized
    }

    /**
     * Normalize and validate a module reference to the 32-byte hash format expected by the SDK.
     *
     * A module reference must be a 32-byte hash represented by 64 hexadecimal characters.
     * WalletConnect InitContract payloads may additionally contain the serialized 4-byte
     * length prefix `00000020`, which is removed before validation.
     *
     * @param moduleRef module reference in hex format, with or without the serialized
     * `00000020` length prefix
     *
     * @return validated 64-character module reference hash in hex format
     *
     * @throws IllegalArgumentException if the module reference has an invalid format
     */
    fun normalizeModuleRef(moduleRef: String): String {
        val normalized =
            if (
                moduleRef.length == 72 &&
                moduleRef.startsWith("00000020")
            ) {
                moduleRef.substring(8)
            } else {
                moduleRef
            }

        require(normalized.length == 64) {
            "Invalid module reference length"
        }

        require(
            normalized.all {
                it in '0'..'9' ||
                    it in 'a'..'f' ||
                    it in 'A'..'F'
            }
        ) {
            "Invalid module reference format"
        }

        return normalized
    }

    /**
     * Validate serialized InitContract parameters provided by a WalletConnect request.
     *
     * Parameters must be represented as an even-length hexadecimal string and must
     * not exceed the maximum parameter size supported by the Concordium SDK.
     *
     * An empty parameter is valid.
     *
     * @param paramHex serialized initialization parameters in hexadecimal format
     *
     * @return validated parameter string
     *
     * @throws IllegalArgumentException if the parameter is malformed or too large
     */
    fun validateInitParamHex(paramHex: String): String {
        require(paramHex.length % 2 == 0) {
            "InitContract parameter must contain an even number of hexadecimal characters"
        }

        require(paramHex.length <= Parameter.MAX_SIZE * 2) {
            "InitContract parameter exceeds the maximum supported size"
        }

        require(
            paramHex.all {
                it in '0'..'9' ||
                    it in 'a'..'f' ||
                    it in 'A'..'F'
            }
        ) {
            "InitContract parameter must contain only hexadecimal characters"
        }

        return paramHex
    }

    /**
     * Validate an amount provided by a WalletConnect InitContract request.
     *
     * Contract initialization amount is encoded as an unsigned 64-bit value
     * representing microCCD.
     *
     * @param amount amount in microCCD
     *
     * @return validated amount
     *
     * @throws IllegalArgumentException if the amount is outside the UInt64 range
     */
    fun validateInitAmount(amount: BigInteger): BigInteger {
        require(amount.signum() >= 0) {
            "InitContract amount must not be negative"
        }

        require(amount <= MAX_UINT64) {
            "InitContract amount exceeds the maximum UInt64 value"
        }

        return amount
    }

    /**
     * Validate max contract execution energy provided by a WalletConnect InitContract request.
     *
     * The value must be non-negative before it is converted to the unsigned
     * representation used by the Concordium SDK.
     *
     * @param maxContractExecutionEnergy maximum contract execution energy
     *
     * @return validated max contract execution energy
     *
     * @throws IllegalArgumentException if the value is negative
     */
    fun validateInitMaxContractExecutionEnergy(
        maxContractExecutionEnergy: Long,
    ): Long {
        require(maxContractExecutionEnergy >= 0) {
            "InitContract maxContractExecutionEnergy must not be negative"
        }

        return maxContractExecutionEnergy
    }

    /**
     * @param payload contract transaction payload
     * @param maxContractExecutionEnergy the corresponding value provided by a dApp
     * @param numSignatures number of signatures, which is 1 unless we implement multisig in the wallet.
     *
     * @return max energy (NRG) which can be spent by this transaction.
     */
    private fun getContractTransactionMaxEnergy(
        payload: Payload,
        maxContractExecutionEnergy: Long,
        numSignatures: Int = 1,
    ): Long = TransactionHeader.calculateMaxEnergyCost(
        numSignatures,
        payload.bytes.size,
        UInt64.from(maxContractExecutionEnergy),
    ).value
}
