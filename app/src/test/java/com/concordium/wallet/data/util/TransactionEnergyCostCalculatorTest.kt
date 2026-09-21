package com.concordium.wallet.data.util

import com.concordium.sdk.transactions.Parameter
import com.concordium.wallet.data.walletconnect.AccountTransactionPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class TransactionEnergyCostCalculatorTest {
    private val moduleRef =
        "f8b8b0acdb6d9c7dd56182ecb701b9a213949f1cad35f0781258464b038116b9"

    private fun payload(
        initName: String = "mint_wizard_000000_V3",
        amount: BigInteger = BigInteger.ZERO,
        maxContractExecutionEnergy: Long = 6000L,
        moduleRef: String = this.moduleRef,
        param: String = "0102",
    ) = AccountTransactionPayload.InitContract.parse(
        initName = initName,
        amount = amount,
        maxContractExecutionEnergy = maxContractExecutionEnergy,
        moduleRef = moduleRef,
        param = param,
    )

    @Test
    fun `parsed WalletConnect payload calculates init transaction energy`() {
        val request = payload(moduleRef = "00000020$moduleRef")

        assertEquals("init_mint_wizard_000000_V3", request.initName)
        assertEquals(moduleRef, request.moduleRef)
        assertTrue(
            TransactionEnergyCostCalculator.getContractTransactionMaxEnergy(
                payload = request.payload,
                maxContractExecutionEnergy = request.maxContractExecutionEnergy,
            ) > request.maxContractExecutionEnergy.value
        )
    }

    @Test
    fun `payload parsing preserves canonical init name`() {
        assertEquals(
            "init_mint_wizard_000000_V3",
            payload(initName = "init_mint_wizard_000000_V3").initName,
        )
    }

    @Test
    fun `payload parsing rejects invalid init names`() {
        listOf("contract.name", "a".repeat(96)).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) {
                payload(initName = invalid)
            }
        }
    }

    @Test
    fun `payload parsing rejects invalid module references`() {
        listOf(
            "g8b8b0acdb6d9c7dd56182ecb701b9a213949f1cad35f0781258464b038116b9",
            "１8b8b0acdb6d9c7dd56182ecb701b9a213949f1cad35f0781258464b038116b9",
        ).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) {
                payload(moduleRef = invalid)
            }
        }
    }

    @Test
    fun `payload parsing accepts valid parameter boundaries`() {
        payload(param = "")
        payload(param = "00".repeat(Parameter.MAX_SIZE))
    }

    @Test
    fun `payload parsing rejects malformed or oversized parameters`() {
        listOf("abc", "00xz", "00".repeat(Parameter.MAX_SIZE + 1)).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) {
                payload(param = invalid)
            }
        }
    }

    @Test
    fun `payload parsing accepts UInt64 amount boundaries`() {
        payload(amount = BigInteger.ZERO)
        payload(amount = BigInteger("18446744073709551615"))
    }

    @Test
    fun `payload parsing rejects amounts outside UInt64 range`() {
        listOf(
            BigInteger.valueOf(-1),
            BigInteger("18446744073709551616"),
        ).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) {
                payload(amount = invalid)
            }
        }
    }

    @Test
    fun `payload parsing rejects empty init name`() {
        assertThrows(IllegalArgumentException::class.java) {
            payload(initName = "")
        }
    }

    @Test
    fun `payload parsing rejects negative execution energy`() {
        listOf(-1L, Long.MIN_VALUE).forEach { invalid ->
            assertThrows("energy=$invalid", IllegalArgumentException::class.java) {
                payload(maxContractExecutionEnergy = invalid)
            }
        }
    }

    @Test
    fun `payload parsing preserves valid execution energy`() {
        listOf(0L, 6000L).forEach { energy ->
            assertEquals(
                energy,
                payload(maxContractExecutionEnergy = energy)
                    .maxContractExecutionEnergy.value,
            )
        }
    }

    @Test
    fun `payload parsing rejects invalid module reference lengths`() {
        listOf(
            "",
            "abcd",
            "a".repeat(63),
            "a".repeat(65),
            "00000021$moduleRef",
        ).forEach { invalid ->
            assertThrows(
                "moduleRef=$invalid",
                IllegalArgumentException::class.java,
            ) {
                payload(moduleRef = invalid)
            }
        }
    }

    @Test
    fun `module reference prefix preserves payload bytes and energy`() {
        val canonical = payload(moduleRef = moduleRef)
        val prefixed = payload(moduleRef = "00000020$moduleRef")

        assertTrue(
            canonical.payload.bytes.contentEquals(prefixed.payload.bytes)
        )
        assertEquals(
            TransactionEnergyCostCalculator.getContractTransactionMaxEnergy(
                payload = canonical.payload,
                maxContractExecutionEnergy = canonical.maxContractExecutionEnergy,
            ),
            TransactionEnergyCostCalculator.getContractTransactionMaxEnergy(
                payload = prefixed.payload,
                maxContractExecutionEnergy = prefixed.maxContractExecutionEnergy,
            ),
        )
    }

    @Test
    fun `hex letter case preserves payload bytes`() {
        val lowercase = payload(
            moduleRef = moduleRef,
            param = "00abcdef",
        )
        val uppercase = payload(
            moduleRef = moduleRef.uppercase(java.util.Locale.ROOT),
            param = "00ABCDEF",
        )

        assertTrue(
            lowercase.payload.bytes.contentEquals(uppercase.payload.bytes)
        )
    }
}
