package com.concordium.wallet.data.util

import com.concordium.sdk.transactions.Parameter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger


class TransactionEnergyCostCalculatorTest {

    @Test
    fun `prefixed and canonical module references produce identical init transaction energy`() {
        val canonicalModuleRef =
            "f8b8b0acdb6d9c7dd56182ecb701b9a213949f1cad35f0781258464b038116b9"

        val prefixedModuleRef =
            "00000020$canonicalModuleRef"

        val canonicalEnergy =
            TransactionEnergyCostCalculator.getContractInitMaxEnergy(
                initName = "mint_wizard_000000_V3",
                amount = BigInteger.ZERO,
                moduleRef = canonicalModuleRef,
                paramHex = "0102",
                maxContractExecutionEnergy = 6000L,
            )

        val prefixedEnergy =
            TransactionEnergyCostCalculator.getContractInitMaxEnergy(
                initName = "mint_wizard_000000_V3",
                amount = BigInteger.ZERO,
                moduleRef = prefixedModuleRef,
                paramHex = "0102",
                maxContractExecutionEnergy = 6000L,
            )

        assertEquals(canonicalEnergy, prefixedEnergy)
    }

    @Test
    fun `getContractInitMaxEnergy accepts WalletConnect Minting Wizard payload`() {
        val moduleRef =
            "00000020f8b8b0acdb6d9c7dd56182ecb701b9a213949f1cad35f0781258464b038116b9"

        val param =
            "010000000101500068747470733a2f2f676174657761792e70696e6174612e636c6f75642f697066732f516d5376386f7a4237745763754e733552467167443946704c4843446b7174737a7954385073696d623757394342000504"

        val maxContractExecutionEnergy = 6000L

        val maxEnergy =
            TransactionEnergyCostCalculator.getContractInitMaxEnergy(
                initName = "mint_wizard_000000_V3",
                amount = BigInteger.ZERO,
                moduleRef = moduleRef,
                paramHex = param,
                maxContractExecutionEnergy = maxContractExecutionEnergy,
            )

        assertTrue(maxEnergy > maxContractExecutionEnergy)
    }

    @Test
    fun `normalizeModuleRef accepts canonical module reference`() {
        val moduleRef =
            "f8b8b0acdb6d9c7dd56182ecb701b9a213949f1cad35f0781258464b038116b9"

        assertEquals(
            moduleRef,
            TransactionEnergyCostCalculator.normalizeModuleRef(moduleRef),
        )
    }

    @Test
    fun `normalizeModuleRef removes serialized length prefix`() {
        val moduleRef =
            "f8b8b0acdb6d9c7dd56182ecb701b9a213949f1cad35f0781258464b038116b9"

        assertEquals(
            moduleRef,
            TransactionEnergyCostCalculator.normalizeModuleRef(
                "00000020$moduleRef",
            ),
        )
    }

    @Test
    fun `normalizeModuleRef rejects invalid length`() {
        assertThrows(IllegalArgumentException::class.java) {
            TransactionEnergyCostCalculator.normalizeModuleRef("abcd")
        }
    }

    @Test
    fun `normalizeModuleRef rejects non hexadecimal characters`() {
        val invalid =
            "g8b8b0acdb6d9c7dd56182ecb701b9a213949f1cad35f0781258464b038116b9"

        assertThrows(IllegalArgumentException::class.java) {
            TransactionEnergyCostCalculator.normalizeModuleRef(invalid)
        }
    }

    @Test
    fun `normalizeModuleRef rejects Unicode digits`() {
        val invalid =
            "１8b8b0acdb6d9c7dd56182ecb701b9a213949f1cad35f0781258464b038116b9"

        assertThrows(IllegalArgumentException::class.java) {
            TransactionEnergyCostCalculator.normalizeModuleRef(invalid)
        }
    }

    @Test
    fun `validateInitParamHex accepts empty parameter`() {
        assertEquals(
            "",
            TransactionEnergyCostCalculator.validateInitParamHex(""),
        )
    }

    @Test
    fun `validateInitParamHex accepts valid hexadecimal parameter`() {
        assertEquals(
            "00abcdefABCDEF",
            TransactionEnergyCostCalculator.validateInitParamHex(
                "00abcdefABCDEF",
            ),
        )
    }

    @Test
    fun `validateInitParamHex rejects odd length`() {
        assertThrows(IllegalArgumentException::class.java) {
            TransactionEnergyCostCalculator.validateInitParamHex("abc")
        }
    }

    @Test
    fun `validateInitParamHex rejects non hexadecimal characters`() {
        assertThrows(IllegalArgumentException::class.java) {
            TransactionEnergyCostCalculator.validateInitParamHex("00xz")
        }
    }

    @Test
    fun `validateInitParamHex accepts maximum supported size`() {
        val param = "00".repeat(Parameter.MAX_SIZE)

        assertEquals(
            param,
            TransactionEnergyCostCalculator.validateInitParamHex(param),
        )
    }

    @Test
    fun `validateInitParamHex rejects parameter exceeding maximum size`() {
        val param = "00".repeat(Parameter.MAX_SIZE + 1)

        assertThrows(IllegalArgumentException::class.java) {
            TransactionEnergyCostCalculator.validateInitParamHex(param)
        }
    }

    @Test
    fun `validateInitAmount accepts zero`() {
        assertEquals(
            BigInteger.ZERO,
            TransactionEnergyCostCalculator.validateInitAmount(
                BigInteger.ZERO,
            ),
        )
    }

    @Test
    fun `validateInitAmount accepts maximum UInt64 value`() {
        val maxUInt64 =
            BigInteger("18446744073709551615")

        assertEquals(
            maxUInt64,
            TransactionEnergyCostCalculator.validateInitAmount(maxUInt64),
        )
    }

    @Test
    fun `validateInitAmount rejects negative value`() {
        assertThrows(IllegalArgumentException::class.java) {
            TransactionEnergyCostCalculator.validateInitAmount(
                BigInteger.valueOf(-1),
            )
        }
    }

    @Test
    fun `validateInitAmount rejects value above UInt64 maximum`() {
        val tooLarge =
            BigInteger("18446744073709551616")

        assertThrows(IllegalArgumentException::class.java) {
            TransactionEnergyCostCalculator.validateInitAmount(tooLarge)
        }
    }

    @Test
    fun `validateInitMaxContractExecutionEnergy accepts zero`() {
        assertEquals(
            0L,
            TransactionEnergyCostCalculator
                .validateInitMaxContractExecutionEnergy(0L),
        )
    }

    @Test
    fun `validateInitMaxContractExecutionEnergy accepts positive value`() {
        assertEquals(
            6000L,
            TransactionEnergyCostCalculator
                .validateInitMaxContractExecutionEnergy(6000L),
        )
    }

    @Test
    fun `validateInitMaxContractExecutionEnergy rejects negative value`() {
        assertThrows(IllegalArgumentException::class.java) {
            TransactionEnergyCostCalculator
                .validateInitMaxContractExecutionEnergy(-1L)
        }
    }

    @Test
    fun `normalizeInitName adds init prefix`() {
        assertEquals(
            "init_mint_wizard_000000_V3",
            TransactionEnergyCostCalculator.normalizeInitName(
                "mint_wizard_000000_V3",
            ),
        )
    }

    @Test
    fun `normalizeInitName preserves existing init prefix`() {
        assertEquals(
            "init_mint_wizard_000000_V3",
            TransactionEnergyCostCalculator.normalizeInitName(
                "init_mint_wizard_000000_V3",
            ),
        )
    }

    @Test
    fun `normalizeInitName rejects empty value`() {
        assertThrows(IllegalArgumentException::class.java) {
            TransactionEnergyCostCalculator.normalizeInitName("")
        }
    }

    @Test
    fun `normalizeInitName rejects dot`() {
        assertThrows(IllegalArgumentException::class.java) {
            TransactionEnergyCostCalculator.normalizeInitName(
                "contract.name",
            )
        }
    }

    @Test
    fun `normalizeInitName rejects name exceeding SDK maximum`() {
        val name = "a".repeat(96)

        assertThrows(IllegalArgumentException::class.java) {
            TransactionEnergyCostCalculator.normalizeInitName(name)
        }
    }
}
