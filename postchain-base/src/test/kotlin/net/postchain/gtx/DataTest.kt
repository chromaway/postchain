// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx

import net.postchain.base.BlockchainRid
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.core.Signature
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import org.junit.Assert.*
import org.junit.Test


fun mustThrowError(msg: String, code: () -> Unit) {
    try {
        code()
        fail(msg)
    } catch (e: Exception) {
    }
}

class GTXDataTest {

    private fun addOperations(b: GTXDataBuilder, signerPub: List<ByteArray>) {
        // primitives
        b.addOperation("hello", arrayOf(GtvNull, gtv(42), gtv("Wow"), gtv(signerPub[0])))
        // array of primitives
        b.addOperation("bro", arrayOf(gtv(GtvNull, gtv(2), gtv("Nope"))))
        // dict
        b.addOperation("dictator", arrayOf(gtv(mapOf("two" to gtv(2), "five" to GtvNull))))
        // complex structure
        b.addOperation("soup", arrayOf(
                // map with array
                gtv(mapOf("array" to gtv(gtv(1), gtv(2), gtv(3)))),
                // array with map
                gtv(gtv(mapOf("inner" to gtv("space"))), GtvNull)
        ))
    }

    @Test
    fun testGTXData() {
        val signerPub = (0..3).map(::pubKey)
        val signerPriv = (0..3).map(::privKey)
        val crypto = SECP256K1CryptoSystem()

        val b = GTXDataBuilder(BlockchainRid.buildRepeat(0), signerPub.slice(0..2).toTypedArray(), crypto)
        addOperations(b, signerPub)
        b.finish()
        b.sign(crypto.buildSigMaker(signerPub[0], signerPriv[0]))

        // try recreating from a serialized copy
        val b2 = GTXDataBuilder.decode(b.serialize(), crypto)
        val txBodyMerkleRoot = b2.getDigestForSigning()
        val sigMaker = crypto.buildSigMaker(signerPub[1], signerPriv[1])
        val signature = sigMaker.signDigest(txBodyMerkleRoot)
        b2.addSignature(signature)

        mustThrowError("Allows duplicate signature") {
            b2.addSignature(signature, true)
        }
        mustThrowError("Allows invalid signature") {
            b2.addSignature(Signature(signerPub[2], signerPub[2]), true)
        }
        mustThrowError("Allows signature from wrong participant") {
            val signatureMaker = crypto.buildSigMaker(signerPub[3], signerPriv[3])
            val wrongSignature = signatureMaker.signDigest(txBodyMerkleRoot)
            b2.addSignature(wrongSignature, true)
        }

        val sigMaker2 = crypto.buildSigMaker(signerPub[2], signerPriv[2])
        b2.sign(sigMaker2)

        assertTrue(b2.isFullySigned())

        //val d = decodeGTXData(b2.serialize())
        val d: GTXTransactionData = decodeGTXTransactionData(b2.serialize())
        val body = d.transactionBodyData

        assertTrue(body.signers.contentDeepEquals(
                signerPub.slice(0..2).toTypedArray()
        ))
        assertEquals(3, d.signatures.size)
        assertEquals(4, body.operations.size)
        assertEquals("bro", body.operations[1].opName)
        val op0 = body.operations[0]
        assertTrue(op0.args[0].isNull())
        assertEquals(42, op0.args[1].asInteger())
        assertEquals("Wow", op0.args[2].asString())
        assertTrue(op0.args[3].asByteArray().contentEquals(signerPub[0]))
        val op1 = body.operations[1]
        assertEquals("Nope", op1.args[0][2].asString())
        val dict2 = body.operations[2].args[0]
        assertEquals(2, dict2["two"]!!.asInteger())
        assertNull(dict2["six"])
        val mapWithArray = body.operations[3].args[0]
        assertEquals(2, mapWithArray["array"]!![1].asInteger())
        val arrayWithMap = body.operations[3].args[1]
        assertEquals("space", arrayWithMap[0]["inner"]!!.asString())
    }
}