// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx.gtxml

import assertk.assert
import assertk.assertions.isEqualTo
import net.postchain.base.BlockchainRid
import net.postchain.devtools.MockCryptoSystem
import net.postchain.gtx.GTXTransactionBodyData
import net.postchain.gtx.GTXTransactionData
import org.junit.Test

class GTXMLTransactionParserBlockchainRIDTest {

    @Test
    fun parseGTXMLTransaction_in_context_with_empty_blockchainRID_successfully() {
        val xml = """
            <transaction blockchainRID="">
                <signers/><operations/><signatures/>
            </transaction>
        """.trimIndent()

        val expectedBody= GTXTransactionBodyData(
                BlockchainRid(byteArrayOf(0x0A, 0x0B, 0x0C)),
                arrayOf(),
                arrayOf())

        val expectedTx = GTXTransactionData(expectedBody, arrayOf())

        val actual = GTXMLTransactionParser.parseGTXMLTransaction(xml,
                TransactionContext(BlockchainRid(byteArrayOf(0x0A, 0x0B, 0x0C))),
                MockCryptoSystem()
        )

        assert(actual).isEqualTo(expectedTx)
    }

    @Test
    fun parseGTXMLTransaction_in_context_with_no_blockchainRID_successfully() {
        val xml = """
            <transaction>
                <signers/><operations/><signatures/>
            </transaction>
        """.trimIndent()

        val expectedBody = GTXTransactionBodyData(
                BlockchainRid(byteArrayOf(0x0A, 0x0B, 0x0C)),
                arrayOf(), arrayOf())

        val expectedTx = GTXTransactionData(expectedBody, arrayOf() )

        val actual = GTXMLTransactionParser.parseGTXMLTransaction(xml,
                TransactionContext(BlockchainRid(byteArrayOf(0x0A, 0x0B, 0x0C))),
                MockCryptoSystem()
        )

        assert(actual).isEqualTo(expectedTx)
    }

    @Test
    fun parseGTXMLTransaction_in_context_with_no_blockchainRID_and_null_context_one_successfully() {
        val xml = """
            <transaction>
                <signers/><operations/><signatures/>
            </transaction>
        """.trimIndent()

        val expectedBody = GTXTransactionBodyData(
                BlockchainRid.EMPTY_RID,
                arrayOf(), arrayOf())

        val expectedTx = GTXTransactionData(expectedBody, arrayOf())

        val actual = GTXMLTransactionParser.parseGTXMLTransaction(xml,
                TransactionContext(null),
                MockCryptoSystem()
                )

        assert(actual).isEqualTo(expectedTx)
    }

    @Test
    fun parseGTXMLTransaction_in_context_with_blockchainRID_equal_to_context_one_successfully() {
        val xml = """
            <transaction blockchainRID="0a0b0c">
                <signers/><operations/><signatures/>
            </transaction>
        """.trimIndent()

        val expectedBody = GTXTransactionBodyData(
                BlockchainRid(byteArrayOf(0x0A, 0x0B, 0x0C)),
                arrayOf(), arrayOf())

        val expectedTx = GTXTransactionData(expectedBody, arrayOf() )

        val actual = GTXMLTransactionParser.parseGTXMLTransaction(xml,
                TransactionContext(null),
                MockCryptoSystem()
        )

        assert(actual).isEqualTo(expectedTx)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseGTXMLTransaction_in_context_with_blockchainRID_not_equal_to_context_one_successfully() {
        val xml = """
            <transaction blockchainRID="0a0b0c">
                <signers/><operations/><signatures/>
            </transaction>
        """.trimIndent()

        val expectedBody = GTXTransactionBodyData(
                BlockchainRid(byteArrayOf(0x0A, 0x0B, 0x0C)),
                arrayOf(), arrayOf())

        val expectedTx = GTXTransactionData(expectedBody, arrayOf() )

        val actual = GTXMLTransactionParser.parseGTXMLTransaction(xml,
                TransactionContext(BlockchainRid(byteArrayOf(0x01, 0x02, 0x03))),
                MockCryptoSystem()
                )

        assert(actual).isEqualTo(expectedTx)
    }
}