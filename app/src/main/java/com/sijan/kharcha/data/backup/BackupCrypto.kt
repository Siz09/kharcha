package com.sijan.kharcha.data.backup

import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {

    private val MAGIC_HEADER = "KHARCHA_ENC_v1\n".toByteArray(StandardCharsets.UTF_8)
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val KEY_LENGTH_BITS = 256
    private const val ITERATIONS = 100_000
    private const val GCM_TAG_LENGTH_BITS = 128

    fun isEncryptedArchive(headerBytes: ByteArray): Boolean {
        if (headerBytes.size < MAGIC_HEADER.size) return false
        return headerBytes.take(MAGIC_HEADER.size).toByteArray().contentEquals(MAGIC_HEADER)
    }

    fun encrypt(
        inputStream: InputStream,
        outputStream: OutputStream,
        passphrase: CharArray
    ) {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { random.nextBytes(it) }

        val keySpec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKey = SecretKeySpec(factory.generateSecret(keySpec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        // Write container header
        outputStream.write(MAGIC_HEADER)
        outputStream.write(salt)
        outputStream.write(iv)

        // Stream encrypted body
        CipherOutputStream(outputStream, cipher).use { cipherOut ->
            inputStream.copyTo(cipherOut)
        }
    }

    fun decrypt(
        inputStream: InputStream,
        outputStream: OutputStream,
        passphrase: CharArray
    ) {
        val magicBuffer = ByteArray(MAGIC_HEADER.size)
        val readMagic = inputStream.read(magicBuffer)
        if (readMagic != MAGIC_HEADER.size || !magicBuffer.contentEquals(MAGIC_HEADER)) {
            throw IllegalArgumentException("Not an encrypted Kharcha archive")
        }

        val salt = ByteArray(SALT_LENGTH_BYTES)
        if (inputStream.read(salt) != SALT_LENGTH_BYTES) {
            throw IllegalArgumentException("Truncated archive header: missing salt")
        }

        val iv = ByteArray(IV_LENGTH_BYTES)
        if (inputStream.read(iv) != IV_LENGTH_BYTES) {
            throw IllegalArgumentException("Truncated archive header: missing IV")
        }

        val keySpec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKey = SecretKeySpec(factory.generateSecret(keySpec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        try {
            CipherInputStream(inputStream, cipher).use { cipherIn ->
                cipherIn.copyTo(outputStream)
            }
        } catch (e: Exception) {
            throw GeneralSecurityException("Decryption failed. Incorrect passphrase or corrupted backup.", e)
        }
    }
}
