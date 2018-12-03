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
}