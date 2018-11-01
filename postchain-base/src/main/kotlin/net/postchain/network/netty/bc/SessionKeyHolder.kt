package net.postchain.network.netty.bc

import net.postchain.base.CURVE
import java.lang.RuntimeException
import java.math.BigInteger
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import java.nio.ByteBuffer

class SessionKeyHolder(val keySizeBytes: Int) {

    private val keyPair = generateKeyPair()

    private var sessionKey: ByteArray? = null

    fun getPublicKey() = keyPair.public.encoded

    fun getSessionKey() = sessionKey ?: throw RuntimeException("Session key is not present yet")

    fun initSessionKey(remotePK: ByteArray) {
        val localPublicKey = keyPair.public.encoded
        val keyFactory = KeyFactory.getInstance("EC")

        val pkSpec = X509EncodedKeySpec(remotePK)
        val otherPublicKey = keyFactory.generatePublic(pkSpec)

        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(keyPair.private)
        keyAgreement.doPhase(otherPublicKey, true)

        val sharedSecret = keyAgreement.generateSecret()

        val hash = MessageDigest.getInstance("SHA-256")
        hash.update(sharedSecret)

        listOf(ByteBuffer.wrap(localPublicKey), ByteBuffer.wrap(remotePK)).sorted().forEach {
            hash.update(it)
        }

        this.sessionKey = hash.digest()
    }
    private fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(keySizeBytes * 8)
        return keyPairGenerator.generateKeyPair()
    }

    fun secp256k1_ecdh(privKey: ByteArray, pubKey: ByteArray): ByteArray {
        val d = BigInteger(1, privKey)
        val Q = CURVE.curve.decodePoint(pubKey)
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(Q.multiply(d).normalize().getEncoded(true))
    }
}