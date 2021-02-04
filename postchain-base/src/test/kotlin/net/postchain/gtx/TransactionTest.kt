// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import net.postchain.base.BlockchainRid
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.core.Transactor
import net.postchain.core.TxEContext
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.math.BigInteger
import kotlin.test.assertFalse

val myCS = SECP256K1CryptoSystem()

fun makeNOPGTX(bcRid: BlockchainRid): ByteArray {
    val b = GTXDataBuilder(bcRid, arrayOf(pubKey(0)), myCS)
    b.addOperation("nop", arrayOf(gtv(42)))
    b.finish()
    b.sign(myCS.buildSigMaker(pubKey(0), privKey(0)))
    return b.serialize()
}

fun makeCorrectNOPGTX(bcRid: BlockchainRid): ByteArray {
    val b = GTXDataBuilder(bcRid, arrayOf(pubKey(0)), myCS)
    b.addOperation("nop", arrayOf(gtv(42)))
    // Need to add a valid dummy operation to make the entire TX valid
    b.addOperation("gtx_test", arrayOf(gtv(1), gtv("true")))
    b.finish()
    b.sign(myCS.buildSigMaker(pubKey(0), privKey(0)))
    return b.serialize()
}

fun makeHackerGTXWithTimeB(bcRid: BlockchainRid): ByteArray {
    val b = GTXDataBuilder(bcRid, arrayOf(pubKey(0)), myCS)
    val arg0 = -(BigInteger.ONE).shiftLeft(64) //-2^64. A too big value, with first 64 LSB bits = 0
    b.addOperation("timeb", arrayOf(gtv(arg0), GtvNull))
    // Need to add a valid dummy operation to make the entire TX valid
    b.addOperation("gtx_test", arrayOf(gtv(1), gtv("true")))
    b.finish()
    b.sign(myCS.buildSigMaker(pubKey(0), privKey(0)))
    return b.serialize()
}

fun makeCorrectGTXWithTimeB(bcRid: BlockchainRid): ByteArray {
    val b = GTXDataBuilder(bcRid, arrayOf(pubKey(0)), myCS)
    val arg0 = -(BigInteger.ONE).shiftLeft(14) //-2^64. A not too big value
    b.addOperation("timeb", arrayOf(gtv(arg0), GtvNull))
    // Need to add a valid dummy operation to make the entire TX valid
    b.addOperation("gtx_test", arrayOf(gtv(1), gtv("true")))
    b.finish()
    b.sign(myCS.buildSigMaker(pubKey(0), privKey(0)))
    return b.serialize()
}

class GTXTransactionTest {

    val standardModule = StandardOpsGTXModule()
    val testModule = mock(GTXModule::class.java)
    val compositModule = CompositeGTXModule(arrayOf(standardModule, testModule), false)

    val gtxDataNop = makeNOPGTX(BlockchainRid.EMPTY_RID)
    val gtxDataCorrectNop = makeCorrectNOPGTX(BlockchainRid.EMPTY_RID)
    val gtxDataTimebHackerAttaque = makeHackerGTXWithTimeB(BlockchainRid.EMPTY_RID)
    val gtxDataTimeb = makeCorrectGTXWithTimeB(BlockchainRid.EMPTY_RID)

    class DummyOperation: Transactor {
        override fun isSpecial() = false
        override fun isCorrect() = true
        override fun apply(ctx: TxEContext) = true
    }

    @Before
    fun setup() {
        whenever(testModule.makeTransactor(any())).thenReturn(DummyOperation())
        whenever(testModule.getOperations()).thenReturn(setOf("gtx_test"))
        compositModule.initializeDB(mock())
    }

    @Test
    fun testNopOnly() {
        val factory = GTXTransactionFactory(BlockchainRid.EMPTY_RID, standardModule, myCS)
        val tx = factory.decodeTransaction(gtxDataNop)
        assertTrue(tx.getRID().size > 1)
        assertTrue(tx.getRawData().size > 1)
        assertTrue((tx as GTXTransaction).ops.size == 1)
        assertFalse(tx.isCorrect())
    }

    @Test
    fun testNopWithOp() {
        val factory = GTXTransactionFactory(BlockchainRid.EMPTY_RID, compositModule, myCS)
        val tx = factory.decodeTransaction(gtxDataCorrectNop)
        assertTrue(tx.getRID().size > 1)
        assertTrue(tx.getRawData().size > 1)
        assertTrue((tx as GTXTransaction).ops.size == 2)
        assertTrue(tx.isCorrect())
    }

    @Test(expected = java.lang.ArithmeticException::class)
    fun testTimeBHackerAttaquetx() {
        val factory = GTXTransactionFactory(BlockchainRid.EMPTY_RID, compositModule, myCS)
        val tx = factory.decodeTransaction(gtxDataTimebHackerAttaque)
        tx.isCorrect()
    }

    @Test
    fun testTimeBCorrectArgs() {
        val factory = GTXTransactionFactory(BlockchainRid.EMPTY_RID, compositModule, myCS)
        val tx = factory.decodeTransaction(gtxDataTimeb)
        assertTrue(tx.getRID().size > 1)
        assertTrue(tx.getRawData().size > 1)
        assertTrue((tx as GTXTransaction).ops.size == 2)
        assertTrue(tx.isCorrect())
    }
}