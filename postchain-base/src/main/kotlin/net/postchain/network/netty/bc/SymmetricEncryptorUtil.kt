package net.postchain.network.netty.bc

import org.spongycastle.jce.provider.BouncyCastleProvider
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import java.security.Security
import java.util.*
import org.apache.commons.lang3.RandomUtils.nextBytes
import java.util.Random
import java.util.concurrent.ThreadLocalRandom


object SymmetricEncryptorUtil {
    private val algorithm = "AES/GCM/NoPadding"
    private val iterations = 2000
    private val keyLength = 256
    private val random = SecureRandom()

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun encrypt(plaintext: ByteArray, passphrase: String, salt: String): ByteArray {
        val key = generateKey(passphrase, salt)
        val cipher = Cipher.getInstance(algorithm)
        val ivBytes = generateIVBytes(cipher)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(ivBytes), random)
        return ivBytes + cipher.doFinal(plaintext)
    }

    fun decrypt(encrypted: ByteArray, passphrase: String, salt: String): ByteArray {
        val key = generateKey(passphrase, salt)
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, key,
                IvParameterSpec(Arrays.copyOfRange(encrypted, 0, 12)),
                random)
        return cipher.doFinal(Arrays.copyOfRange(encrypted, 12,
                encrypted.size))
    }

    fun generatePassphrase(size: Int): ByteArray {
        val random = ThreadLocalRandom.current()
        val randomBytes = ByteArray(size)
        random.nextBytes(randomBytes)
        return randomBytes
    }

    private fun generateKey(passphrase: String, salt: String): SecretKey {
        val keySpec = PBEKeySpec(passphrase.toCharArray(),
                salt.toByteArray(), iterations, keyLength)
        val keyFactory = SecretKeyFactory.getInstance("PBEWITHSHA256AND256BITAES-CBC-BC")
        return keyFactory.generateSecret(keySpec)
    }

    private fun generateIVBytes(cipher: Cipher): ByteArray {
        val ivBytes = ByteArray(12)
        random.nextBytes(ivBytes)

        return ivBytes
    }
}