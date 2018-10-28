package net.postchain.network.netty.bc

import org.junit.Assert
import org.junit.Test

class SymmetricEncryptionTest {

    @Test
    fun testEncryptDecrypt() {

        val passphrase = "passphrase"
        val salt = "salt"
        val message = "test".toByteArray()
        val encryptedText = SymmetricEncryptorUtil.encrypt(message, passphrase, salt)

        Assert.assertArrayEquals(message, SymmetricEncryptorUtil.decrypt(encryptedText, passphrase, salt))
    }
}