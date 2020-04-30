package net.postchain.integrationtest.statemachine

import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import kotlin.test.assertEquals

class FailedTestTransactionFactoryTest {

    @Test
    fun testFailedFactory() {
        val strategy = FailedTestTransactionFactory()

        val encodedTx0 = encodedTx(0)
        val tx0 = strategy.decodeTransaction(encodedTx0)
        assertEquals(tx0.isCorrect(), true)

        val encodedTx1 = encodedTx(1)
        val tx1 = strategy.decodeTransaction(encodedTx1)
        assertEquals(tx1.isCorrect(), false)

        val encodedTx2 = encodedTx(2)
        val tx2 = strategy.decodeTransaction(encodedTx2)
        assertEquals(tx2.isCorrect(), true)

        val encodedTx3 = encodedTx(3)
        val tx3 = strategy.decodeTransaction(encodedTx3)
        assertEquals(tx3.isCorrect(), false)
    }

    @Test
    fun testNotFailedFactory() {
        val strategy = NotFailedTestTransactionFactory()

        val encodedTx0 = encodedTx(0)
        val tx0 = strategy.decodeTransaction(encodedTx0)
        assertEquals(tx0.isCorrect(), true)

        val encodedTx1 = encodedTx(1)
        val tx1 = strategy.decodeTransaction(encodedTx1)
        assertEquals(tx1.isCorrect(), true)

        val encodedTx2 = encodedTx(2)
        val tx2 = strategy.decodeTransaction(encodedTx2)
        assertEquals(tx2.isCorrect(), true)

        val encodedTx3 = encodedTx(3)
        val tx3 = strategy.decodeTransaction(encodedTx3)
        assertEquals(tx3.isCorrect(), true)
    }

    private fun encodedTx(id: Int): ByteArray {
        val byteStream = ByteArrayOutputStream(4)
        val dataStream = DataOutputStream(byteStream)
        dataStream.writeInt(id)
        dataStream.flush()
        return byteStream.toByteArray()
    }

}