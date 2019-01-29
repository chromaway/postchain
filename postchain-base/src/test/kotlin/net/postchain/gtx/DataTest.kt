// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.gtx

import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.common.hexStringToByteArray
import net.postchain.core.Signature
import net.postchain.gtv.*
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import org.junit.Assert.*
import org.junit.Test

// TODO: figure out why we get different results
// val testBlockchainRID = SECP256K1CryptoSystem().digest("Test blockchainRID".toByteArray())
val testBlockchainRID = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3".hexStringToByteArray()

fun mustThrowError(msg: String, code: () -> Unit) {
    try {
        code()
        fail(msg)
    } catch (e: Exception) {
    }
}

class GTXDataTest {

    @Test
    fun testGTXData() {
        val signerPub = (0..3).map(::pubKey)
        val signerPriv = (0..3).map(::privKey)
        val crypto = SECP256K1CryptoSystem()

        val b = GTXDataBuilder(testBlockchainRID, signerPub.slice(0..2).toTypedArray(), crypto)
        // primitives
        b.addOperation("hello", arrayOf(GtvNull, gtv(42), gtv("Wow"), gtv(signerPub[0])))
        // args of primitives
        b.addOperation("bro", arrayOf(gtv(GtvNull, gtv(2), gtv("Nope"))))
        // dict
        b.addOperation("dictator", arrayOf(gtv(mapOf("two" to gtv(2), "five" to GtvNull))))
        // complex structure
        b.addOperation("soup", arrayOf(
                // map with args
                gtv(mapOf("args" to gtv(gtv(1), gtv(2), gtv(3)))),
                // args with map
                gtv(gtv(mapOf("inner" to gtv("space"))), GtvNull)
        ))
        b.finish()
        b.sign(crypto.makeSigner(signerPub[0], signerPriv[0]))

        // try recreating from a serialized copy
        val b2 = GTXDataBuilder.decode(b.serialize(), crypto)
        val dataForSigning = b2.getDataForSigning()
        val signature = crypto.makeSigner(signerPub[1], signerPriv[1])(dataForSigning)
        b2.addSignature(signature)

        mustThrowError("Allows duplicate signature") {
            b2.addSignature(signature, true)
        }
        mustThrowError("Allows invalid signature") {
            b2.addSignature(Signature(signerPub[2], signerPub[2]), true)
        }
        mustThrowError("Allows signature from wrong participant") {
            val wrongSignature = crypto.makeSigner(signerPub[3], signerPriv[3])(dataForSigning)
            b2.addSignature(wrongSignature, true)
        }

        b2.sign(crypto.makeSigner(signerPub[2], signerPriv[2]))

        assertTrue(b2.isFullySigned())

        val d = decodeGTXData(b2.serialize())

        assertTrue(d.signers.contentDeepEquals(
                signerPub.slice(0..2).toTypedArray()
        ))
        assertEquals(3, d.signatures.size)
        assertEquals(4, d.operations.size)
        assertEquals("bro", d.operations[1].opName)
        val op0 = d.operations[0]
        assertTrue(op0.args[0].isNull())
        assertEquals(42, op0.args[1].asInteger())
        assertEquals("Wow", op0.args[2].asString())
        assertTrue(op0.args[3].asByteArray().contentEquals(signerPub[0]))
        val op1 = d.operations[1]
        assertEquals("Nope", op1.args[0][2].asString())
        val dict2 = d.operations[2].args[0]
        assertEquals(2, dict2["two"]!!.asInteger())
        assertNull(dict2["six"])
        val mapWithArray = d.operations[3].args[0]
        assertEquals(2, mapWithArray["args"]!![1].asInteger())
        val arrayWithMap = d.operations[3].args[1]
        assertEquals("space", arrayWithMap[0]["inner"]!!.asString())
    }
}