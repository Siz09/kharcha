package com.sijan.kharcha.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException

class BackupCryptoTest {

    @Test
    fun encryptAndDecrypt_roundtripRecoversOriginalData() {
        val originalText = "Kharcha Offline Financial Ledger Data - Encrypted Payload 123456"
        val originalBytes = originalText.toByteArray(StandardCharsets.UTF_8)
        val passphrase = "SecureNepaliPassword123#".toCharArray()

        // Encrypt
        val cipherOut = ByteArrayOutputStream()
        BackupCrypto.encrypt(
            inputStream = ByteArrayInputStream(originalBytes),
            outputStream = cipherOut,
            passphrase = passphrase
        )
        val encryptedBytes = cipherOut.toByteArray()

        assertTrue(encryptedBytes.isNotEmpty())
        assertTrue(BackupCrypto.isEncryptedArchive(encryptedBytes))

        // Decrypt
        val decryptedOut = ByteArrayOutputStream()
        BackupCrypto.decrypt(
            inputStream = ByteArrayInputStream(encryptedBytes),
            outputStream = decryptedOut,
            passphrase = passphrase
        )
        val decryptedBytes = decryptedOut.toByteArray()

        assertArrayEquals(originalBytes, decryptedBytes)
        assertEquals(originalText, String(decryptedBytes, StandardCharsets.UTF_8))
    }

    @Test
    fun decrypt_wrongPassphrase_throwsGeneralSecurityException() {
        val originalBytes = "Secret Data".toByteArray(StandardCharsets.UTF_8)
        val correctPassphrase = "CorrectPassword123".toCharArray()
        val wrongPassphrase = "WrongPassword456".toCharArray()

        val cipherOut = ByteArrayOutputStream()
        BackupCrypto.encrypt(
            inputStream = ByteArrayInputStream(originalBytes),
            outputStream = cipherOut,
            passphrase = correctPassphrase
        )
        val encryptedBytes = cipherOut.toByteArray()

        val decryptedOut = ByteArrayOutputStream()
        assertThrows(GeneralSecurityException::class.java) {
            BackupCrypto.decrypt(
                inputStream = ByteArrayInputStream(encryptedBytes),
                outputStream = decryptedOut,
                passphrase = wrongPassphrase
            )
        }
    }

    @Test
    fun isEncryptedArchive_detectsNonEncryptedDataCorrectly() {
        val plainZipHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x00) // "PK.."
        assertFalse(BackupCrypto.isEncryptedArchive(plainZipHeader))
    }
}
