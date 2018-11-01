package net.postchain.network.netty.bc

import net.postchain.network.netty.DecodedMessageHolder
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import java.security.Security
import java.util.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


object SymmetricEncryptorUtil {
    private val algorithm = "AES/GCM/NoPadding"
    private val iterations = 2000
    private val keyLength = 256
    private val random = SecureRandom()
    private val salt = "static".toByteArray()
    private val ivArraySize = Integer.BYTES + java.lang.Long.BYTES

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun encrypt(plaintext: ByteArray, sessionKey: ByteArray, serial: Long): ByteArray {
        val key = generateKey(sessionKey)
        val cipher = Cipher.getInstance(algorithm)
        val ivBytes = generateIVBytes(serial)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, ivBytes), random)
        return ivBytes + cipher.doFinal(plaintext)
    }

    fun decrypt(encrypted: ByteArray, sessionKey: ByteArray): DecodedMessageHolder {
        val key = generateKey(sessionKey)
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, key,
                GCMParameterSpec(128, Arrays.copyOfRange(encrypted, 0, ivArraySize)),
                random)
        val byteBuffer = ByteBuffer.allocate(ivArraySize)
        byteBuffer.put(cipher.iv, 0, cipher.iv.size)
        byteBuffer.flip()
        byteBuffer.getInt()
        return DecodedMessageHolder(byteArray = cipher.doFinal(Arrays.copyOfRange(encrypted, ivArraySize,
                encrypted.size)),
                serial = byteBuffer.getLong())
    }

    private fun generateKey(sessionKey: ByteArray) = SecretKeySpec(sessionKey, "AES")
//    : SecretKey {
//        val keySpec = PBEKeySpec(sessionKey.toCharArray(), salt, iterations, keyLength)
//        val keyFactory = SecretKeyFactory.getInstance("PBEWITHSHA256AND256BITAES-CBC-BC")
//        return keyFactory.generateSecret(keySpec)
//    }

    private fun generateIVBytes(serial: Long) =
            ByteBuffer.allocate(ivArraySize)
                      .putInt(1)
                      .putLong(serial)
                      .array()
}