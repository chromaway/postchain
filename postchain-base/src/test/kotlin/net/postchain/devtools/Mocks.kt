// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools

import net.postchain.base.CryptoSystem
import net.postchain.base.SigMaker
import net.postchain.base.Verifier
import net.postchain.common.data.Hash
import net.postchain.base.secp256k1_verify
import net.postchain.core.Signature
import java.security.MessageDigest
import kotlin.experimental.xor


class MockSigMaker(val pubKey: ByteArray, val privKey: ByteArray, val digestFun: (ByteArray) -> Hash): SigMaker {
    override fun signMessage(msg: ByteArray): Signature {
        val digestMsg = digestFun(msg)
        return signDigest(digestMsg)
    }
    override fun signDigest(digest: Hash): Signature {
        digest.forEachIndexed { index, byte -> byte xor pubKey[index] }
        return Signature(pubKey, digest)
    }
}

class MockCryptoSystem : CryptoSystem {

    override fun digest(bytes: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes)
    }

    override fun buildSigMaker(pubKey: ByteArray, privKey: ByteArray): SigMaker {
        return MockSigMaker(pubKey, privKey, ::digest)
    }

    override fun verifyDigest(ddigest: ByteArray, s: Signature): Boolean {
        return secp256k1_verify(ddigest, s.subjectID, s.data)
    }

    override fun makeVerifier(): Verifier {
        return { data, signature: Signature ->
            secp256k1_verify(digest(data), signature.subjectID, signature.data)
        }
    }

    override fun getRandomBytes(size: Int): ByteArray {
        return ByteArray(size)
    }
}