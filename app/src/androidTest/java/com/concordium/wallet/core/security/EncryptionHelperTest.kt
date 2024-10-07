package com.concordium.wallet.core.security

import com.concordium.wallet.data.model.EncryptedData
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
}
