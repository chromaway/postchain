// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import net.postchain.base.BlockchainRid
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.common.hexStringToByteArray
import net.postchain.core.Transaction
import net.postchain.core.Transactor
import net.postchain.core.TxEContext
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.math.BigInteger
import kotlin.test.assertFalse

class GTXTransactionTest {

    val myCS = SECP256K1CryptoSystem()
    val standardModule = StandardOpsGTXModule()
    val testModule = mock(GTXModule::class.java)
    val compositModule = CompositeGTXModule(arrayOf(standardModule, testModule), false)


    class DummyOperation : Transactor {
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
        val operationList = listOf(Pair("nop", arrayOf<Gtv>(gtv(42))))
        val gtx = makeGTX(operationList)
        val tx = factory.decodeTransaction(gtx)
        assertTrue(tx.getRID().size > 1)
        assertTrue(tx.getRawData().size > 1)
        assertTrue((tx as GTXTransaction).ops.size == 1)
        assertFalse(tx.isCorrect())
    }

    @Test
    fun testNopWithOp() {
        // gtx_test: Need to add a valid dummy operation to make the entire TX valid
        val operationList = listOf(Pair("nop", arrayOf<Gtv>(gtv(42))),
                Pair("gtx_test", arrayOf<Gtv>(gtv(1), gtv("true"))))
        val tx = makeTransaction(operationList)
        assertTrue((tx as GTXTransaction).ops.size == 2)
        assertTrue(tx.isCorrect())
    }

    @Test
    fun testNop2ArgsWithOp() {
        // gtx_test: Need to add a valid dummy operation to make the entire TX valid
        val operationList = listOf(Pair("nop", arrayOf<Gtv>(gtv(42), gtv(43))),
                Pair("gtx_test", arrayOf<Gtv>(gtv(1), gtv("true"))))
        val tx = makeTransaction(operationList)
        assertFalse(tx.isCorrect())
    }


    @Test(expected = java.lang.ArithmeticException::class)
    fun testTimeBHackerAttack() {
        val tx = timeBTransaction(64) //-2^64. A too big value fit in a Long
        tx.isCorrect()
    }

    @Test
    fun testTimeBCorrectArgs() {
        val tx = timeBTransaction(14) //-2^14 is a not too big value for a Long
        assertTrue(tx.isCorrect())
    }

    @Test
    fun testNopOKString() {
        val okString  = "1234567890123456789012345678901234567890123456789012345678901234"
        val operationList = listOf(Pair("nop", arrayOf<Gtv>(gtv(okString))),
                Pair("gtx_test", arrayOf<Gtv>(gtv(1), gtv("true"))))
        val tx = makeTransaction(operationList)
        assertTrue(tx.isCorrect())
    }

    @Test
    fun testNopOKByteArray() {
        val okByteArray = ("00010203040506070809101112131415" +
                "00010203040506070809101112131415" +
                "00010203040506070809101112131415" +
                "00010203040506070809101112131415").hexStringToByteArray()
        val operationList = listOf(Pair("nop", arrayOf<Gtv>(gtv(okByteArray))),
                Pair("gtx_test", arrayOf<Gtv>(gtv(1), gtv("true"))))
        val tx = makeTransaction(operationList)
        assertTrue(tx.isCorrect())
    }

    @Test
    fun testNopTooBigByteArray() {
        val badByteArray = ("00010203040506070809101112131415" +
                "00010203040506070809101112131415" +
                "00010203040506070809101112131415" +
                "0001020304050607080910111213141500").hexStringToByteArray()  // > 64 bytes
        val operationList = listOf(Pair("nop", arrayOf<Gtv>(gtv(badByteArray))),
                Pair("gtx_test", arrayOf<Gtv>(gtv(1), gtv("true"))))
        val tx = makeTransaction(operationList)
        assertFalse(tx.isCorrect())
    }

    @Test
    fun testNopTooBigString() {
        val badString = "12345678901234567890123456789012345678901234567890123456789012345" // > 64 chars
        val operationList = listOf(Pair("nop", arrayOf<Gtv>(gtv(badString))),
                Pair("gtx_test", arrayOf<Gtv>(gtv(1), gtv("true"))))
        val tx = makeTransaction(operationList)
        assertFalse(tx.isCorrect())
    }

    private fun makeGTX(opList: List<Pair<String, Array<Gtv>>>): ByteArray {
        val b = GTXDataBuilder(BlockchainRid.EMPTY_RID, arrayOf(pubKey(0)), myCS)
        for (operation in opList) {
            b.addOperation(operation.first, operation.second)
        }
        b.finish()
        b.sign(myCS.buildSigMaker(pubKey(0), privKey(0)))
        return b.serialize()
    }

    private fun timeBTransaction(i: Int): Transaction {
        val arg0 = -(BigInteger.ONE).shiftLeft(i) //-2^i
        // gtx_test: Need to add a valid dummy operation to make the entire TX valid
        val operationList = listOf(Pair("timeb", arrayOf<Gtv>(gtv(arg0), GtvNull)),
                Pair("gtx_test", arrayOf<Gtv>(gtv(1), gtv("true"))))
        return makeTransaction(operationList)
    }

    private fun makeTransaction(operationList: List<Pair<String, Array<Gtv>>>): Transaction {
        val gtx = makeGTX(operationList)
        val factory = GTXTransactionFactory(BlockchainRid.EMPTY_RID, compositModule, myCS)
        return factory.decodeTransaction(gtx)
    }
}