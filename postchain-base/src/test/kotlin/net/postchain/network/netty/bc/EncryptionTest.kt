package net.postchain.network.netty.bc

import org.junit.Assert
import org.junit.Test
import java.lang.RuntimeException
import java.util.*

class EncryptionTest {

    @Test
    fun testEncryptDecrypt() {
        val sessionKey = ByteArray(32)
        val message = "initSessionKey".toByteArray()
        val serial = 2L
        val encryptedText = SymmetricEncryptorUtil.encrypt(message, sessionKey, serial)

        val decodedMessage = SymmetricEncryptorUtil.decrypt(encryptedText, sessionKey)
        Assert.assertArrayEquals(message, decodedMessage.byteArray)
        Assert.assertEquals(serial, decodedMessage.serial)
    }

    @Test
    fun testDiffieHellmanPositive() {
        val a = SessionKeyHolder(32)

        val b = SessionKeyHolder(32)
        b.initSessionKey(a.getPublicKey())
        a.initSessionKey(b.getPublicKey())

        Assert.assertArrayEquals(a.getSessionKey(), b.getSessionKey())
    }

    @Test(expected = RuntimeException::class)
    fun testDiffieHellmanNotInitializedKey() {
        val a = SessionKeyHolder(32)

        val b = SessionKeyHolder(32)
        b.initSessionKey(a.getPublicKey())

        Assert.assertArrayEquals(a.getSessionKey(), b.getSessionKey())
    }

    @Test
    fun testDiffieHellmanNegative() {
        val a = SessionKeyHolder(32)

        val b = SessionKeyHolder(32)

        val wrongData = ByteArray(3)
        b.initSessionKey(a.getPublicKey() + wrongData)
        a.initSessionKey(b.getPublicKey())

        Assert.assertFalse(Arrays.equals(a.getSessionKey(), b.getSessionKey()))
    }
}