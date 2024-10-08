package com.concordium.wallet.core.security

import com.concordium.wallet.data.model.EncryptedData
import com.concordium.wallet.util.toHex
import com.walletconnect.util.hexToBytes
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

class EncryptionHelperTest {
    private val sharedKey = "12345678912345678912345678912345"
        .hexToBytes()

    @Test
    fun encryptSuccessfully() {
        val data = "This should be kept in secret 🤫".toByteArray()
        val (ed1, ed2) = runBlocking {
            List(2) {
                EncryptionHelper.encrypt(
                    key = sharedKey,
                    data = data,
                ).also(::println)
            }
        }

        Assert.assertNotEquals(
            "Cipher text must not be repeated",
            ed1.ciphertext, ed2.ciphertext
        )
        Assert.assertNotEquals(
            "IV must not be reused",
            ed1.iv, ed2.iv
        )
        Assert.assertEquals(
            "Transformation must remain constant",
            ed1.transformation, ed2.transformation
        )
        Assert.assertEquals("AES/GCM/NoPadding", ed1.transformation)
    }

    @Test
    fun encryptSuccessfully_IfNoData() {
        val ed = runBlocking {
            EncryptionHelper.encrypt(
                key = sharedKey,
                data = byteArrayOf(),
            ).also(::println)
        }

        Assert.assertEquals(
            "Only the 128 bit tag must be present in the cipher text",
            128 / 8, ed.decodeCipherText().size
        )
    }

    @Test
    fun decryptSuccessfully() {
        val ed = EncryptedData(
            ciphertext = "YQQvlr6O/Wr5YnLAS1wlNql6P/ovePjGe6ebH5tchLQ6Z9jz6aFAFONCktsFGihxlg4",
            transformation = "AES/GCM/NoPadding",
            iv = "QkQj9YofBqR7actQ",
        )

        val data = runBlocking {
            EncryptionHelper.decrypt(
                key = sharedKey,
                encryptedData = ed,
            )
        }

        Assert.assertEquals(
            "This should be kept in secret 🤫",
            String(data)
        )
    }

    @Test
    fun decryptSuccessfully_IfLegacyTransformation() {
        val ed = EncryptedData(
            ciphertext = "P1ZQet2bjxGmpGkzKOM680jgtnA+UOAo5ZYB3q7mwW7euZxMwwGF/yGtNm6zBYK3",
            transformation = "AES/CBC/PKCS7Padding",
            iv = "92b5AYQVTTr0fQXgmUAIAQ",
        )

        val data = runBlocking {
            EncryptionHelper.decrypt(
                key = sharedKey,
                encryptedData = ed,
            )
        }

        Assert.assertEquals(
            "This should be kept in secret 🤫",
            String(data)
        )
    }

    @Test(expected = EncryptionException::class)
    fun failToDecrypt_IfDifferentKey() {
        val ed = EncryptedData(
            ciphertext = "YQQvlr6O/Wr5YnLAS1wlNql6P/ovePjGe6ebH5tchLQ6Z9jz6aFAFONCktsFGihxlg4",
            transformation = "AES/GCM/NoPadding",
            iv = "QkQj9YofBqR7actQ",
        )

        runBlocking {
            EncryptionHelper.decrypt(
                key = byteArrayOf(1, 2, 3),
                encryptedData = ed,
            )
        }
    }

    @Test(expected = EncryptionException::class)
    fun failToDecrypt_IfDifferentIv() {
        val ed = EncryptedData(
            ciphertext = "YQQvlr6O/Wr5YnLAS1wlNql6P/ovePjGe6ebH5tchLQ6Z9jz6aFAFONCktsFGihxlg4",
            transformation = "AES/GCM/NoPadding",
            iv = "QkQj9YofCdR7actQ",
        )

        runBlocking {
            EncryptionHelper.decrypt(
                key = sharedKey,
                encryptedData = ed,
            )
        }
    }

    @Test(expected = EncryptionException::class)
    fun failToDecrypt_IfAlteredCipherText() {
        val ed = EncryptedData(
            ciphertext = "YQQvlr6O/Wr5YnLAS1wlNql6P/ovmPjGe6ebH5tchLQ6Z9jz6aFAFONCktsFGihxlg4",
            transformation = "AES/GCM/NoPadding",
            iv = "QkQj9YofBqR7actQ",
        )

        runBlocking {
            EncryptionHelper.decrypt(
                key = sharedKey,
                encryptedData = ed,
            )
        }
    }

    @Test
    fun generateMasterKeySuccessfully() {
        val (mc1, mc2) = runBlocking {
            List(2) {
                EncryptionHelper.generateMasterKey().toHex().also(::println)
            }
        }

        Assert.assertNotEquals(
            "Generated keys must not be repeated",
            mc1, mc2
        )
        Assert.assertEquals(
            "Generated key size must remain constant",
            mc1.length, mc2.length
        )
        Assert.assertTrue(
            "Generated keys must be strong",
            mc1.length >= 64,
        )
    }

    @Test
    fun generatePasswordKeySaltSuccessfully() {
        val (s1, s2) = runBlocking {
            List(2) {
                EncryptionHelper.generatePasswordKeySalt().toHex().also(::println)
            }
        }

        Assert.assertNotEquals(
            "Generated salt must not be repeated",
            s1, s2
        )
    }

    @Test
    fun generatePasswordKeySuccessfully() {
        val p1 = "123456".toCharArray()
        val p2 = "qwe123".toCharArray()
        val s1 = "7804836792fbecf04961fe286e8ec950d2e105448e61a290262711154ec59026".hexToBytes()
        val s2 = "ac6e5196afd67b0e69c42fcba198420391b79a6ba9a916fa76a905f09017acc5".hexToBytes()

        val (k1, k2) = runBlocking {
            List(2) {
                EncryptionHelper.generatePasswordKey(
                    password = p1,
                    salt = s1,
                ).toHex().also(::println)
            }
        }

        Assert.assertEquals(
            "Generated keys must remain constant if the inputs are constant",
            k1, k2
        )
        Assert.assertTrue(
            "Generated keys must be strong",
            k1.length >= 64
        )

        val k1s2 = runBlocking {
            EncryptionHelper.generatePasswordKey(
                password = p1,
                salt = s2,
            ).toHex()
        }

        Assert.assertNotEquals(
            "Keys generated with the same password but different salt must be different",
            k1, k1s2
        )

        val k2s1 = runBlocking {
            EncryptionHelper.generatePasswordKey(
                password = p2,
                salt = s1,
            ).toHex()
        }

        Assert.assertNotEquals(
            "Keys generated with the same salt but different passwords must be different",
            k1, k2s1
        )
    }
}
