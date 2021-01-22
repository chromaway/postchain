// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx.gtxml

import assertk.assert
import assertk.assertions.isEqualTo
import net.postchain.base.BlockchainRid
import net.postchain.core.UserMistake
import net.postchain.devtools.MockCryptoSystem
import net.postchain.gtv.*
import net.postchain.gtx.GTXTransactionBodyData
import net.postchain.gtx.GTXTransactionData
import net.postchain.gtx.OpData
import org.junit.Test

class GTXMLTransactionParserSignersSignaturesAndOperationsTest {
    val blockchainRID = BlockchainRid.buildFromHex("1234567812345678123456781234567812345678123456781234567812345678")

    @Test
    fun parseGTXMLTransaction_successfully() {
        val xml = javaClass.getResource("/net/postchain/gtx/gtxml/parse/tx_full.xml").readText()

        val expectedBody = GTXTransactionBodyData(
                blockchainRID,
                arrayOf(
                        OpData("ft_transfer",
                                arrayOf(
                                        GtvString("hello"),
                                        GtvString("hello2"),
                                        GtvString("hello3"),
                                        GtvInteger(42),
                                        GtvInteger(43))),
                        OpData("ft_transfer",
                                arrayOf(
                                        GtvString("HELLO"),
                                        GtvString("HELLO2"),
                                        GtvInteger(142),
                                        GtvInteger(143)))
                ),
                arrayOf(
                        byteArrayOf(0x12, 0x38, 0x71, 0x23),
                        byteArrayOf(0x12, 0x38, 0x71, 0x24)
                )
        )

        val expectedTx = GTXTransactionData(expectedBody,
                arrayOf(
                        byteArrayOf(0x34, 0x56, 0x78, 0x54),
                        byteArrayOf(0x34, 0x56, 0x78, 0x55)
                )
        )

        val actual = GTXMLTransactionParser.parseGTXMLTransaction(xml, TransactionContext.empty(), MockCryptoSystem())

        assert(actual).isEqualTo(expectedTx)
    }

    @Test
    fun parseGTXMLTransaction_with_empty_all_sections_successfully() {
        val xml = javaClass.getResource("/net/postchain/gtx/gtxml/parse/tx_empty.xml").readText()

        val expectedBody = GTXTransactionBodyData(
                blockchainRID,
                arrayOf(),
                arrayOf()
        )

        val expectedTx = GTXTransactionData(expectedBody, arrayOf())

        val actual = GTXMLTransactionParser.parseGTXMLTransaction(xml, TransactionContext.empty(), MockCryptoSystem())

        assert(actual).isEqualTo(expectedTx)
    }

    @Test
    fun parseGTXMLTransaction_with_empty_signers_and_signatures_successfully() {
        val xml = javaClass.getResource("/net/postchain/gtx/gtxml/parse/tx_empty_signers_and_signatures.xml").readText()

        val expectedBody = GTXTransactionBodyData(
                blockchainRID,
                arrayOf(
                        OpData("ft_transfer",
                                arrayOf(
                                        GtvString("hello"),
                                        GtvString("hello2"),
                                        GtvString("hello3"),
                                        GtvInteger(42),
                                        GtvInteger(43))),
                        OpData("ft_transfer",
                                arrayOf(
                                        GtvString("HELLO"),
                                        GtvString("HELLO2"),
                                        GtvInteger(142),
                                        GtvInteger(143)))
                ),
                arrayOf()
        )

        val expectedTx = GTXTransactionData(expectedBody, arrayOf())

        val actual = GTXMLTransactionParser.parseGTXMLTransaction(xml, TransactionContext.empty(), MockCryptoSystem())

        assert(actual).isEqualTo(expectedTx)
    }

    @Test
    fun parseGTXMLTransaction_with_empty_operations_successfully() {
        val xml = javaClass.getResource("/net/postchain/gtx/gtxml/parse/tx_empty_operations.xml").readText()

        val expectedBody = GTXTransactionBodyData(
                blockchainRID,
                arrayOf(),
                arrayOf(
                        byteArrayOf(0x12, 0x38, 0x71, 0x23),
                        byteArrayOf(0x12, 0x38, 0x71, 0x24)
                )
        )
        val expectedTx = GTXTransactionData(expectedBody,
                arrayOf(
                        byteArrayOf(0x34, 0x56, 0x78, 0x54),
                        byteArrayOf(0x34, 0x56, 0x78, 0x55)
                )
        )

        val actual = GTXMLTransactionParser.parseGTXMLTransaction(xml, TransactionContext.empty(), MockCryptoSystem())

        assert(actual).isEqualTo(expectedTx)
    }

    @Test
    fun parseGTXMLTransaction_with_empty_operation_parameters_successfully() {
        val xml = javaClass.getResource("/net/postchain/gtx/gtxml/parse/tx_empty_operation_parameters.xml").readText()

        val expectedBody = GTXTransactionBodyData(
                blockchainRID,
                arrayOf(OpData("ft_transfer", arrayOf())),
                arrayOf(
                        byteArrayOf(0x12, 0x38, 0x71, 0x23),
                        byteArrayOf(0x12, 0x38, 0x71, 0x24)
                )
        )

        val expectedTx = GTXTransactionData(expectedBody,
                arrayOf(
                        byteArrayOf(0x34, 0x56, 0x78, 0x54),
                        byteArrayOf(0x34, 0x56, 0x78, 0x55)
                )
        )

        val actual = GTXMLTransactionParser.parseGTXMLTransaction(xml, TransactionContext.empty(), MockCryptoSystem())

        assert(actual).isEqualTo(expectedTx)
    }

    @Test
    fun parseGTXMLTransaction_in_context_with_params_in_all_sections_successfully() {
        val xml = javaClass.getResource("/net/postchain/gtx/gtxml/parse/tx_full_params.xml").readText()

        val expectedBody = GTXTransactionBodyData(
                blockchainRID,
                arrayOf(OpData("ft_transfer",
                        arrayOf(GtvString("hello"),
                                GtvString("my string param"),
                                GtvInteger(123),
                                GtvByteArray(byteArrayOf(0x0A, 0x0B, 0x0C)))
                )),
                arrayOf(
                        byteArrayOf(0x12, 0x38, 0x71, 0x23),
                        byteArrayOf(0x12, 0x38, 0x71, 0x24),
                        byteArrayOf(0x01, 0x02, 0x03)
                )
        )

        val expectedTx = GTXTransactionData(expectedBody,
                arrayOf(
                        byteArrayOf(0x34, 0x56, 0x78, 0x54),
                        byteArrayOf(0x0A, 0x0B, 0x0C, 0x0D),
                        byteArrayOf(0x0E, 0x0F)
                )
        )

        val context = TransactionContext(
                null,
                mapOf(
                        "param_signer" to GtvByteArray(byteArrayOf(0x01, 0x02, 0x03)),

                        "param_string" to GtvString("my string param"),
                        "param_int" to GtvInteger(123),
                        "param_bytearray" to GtvByteArray(byteArrayOf(0x0A, 0x0B, 0x0C)),

                        "param_signature_1" to GtvByteArray(byteArrayOf(0x0A, 0x0B, 0x0C, 0x0D)),
                        "param_signature_2" to GtvByteArray(byteArrayOf(0x0E, 0x0F))
                )
        )

        val actual = GTXMLTransactionParser.parseGTXMLTransaction(xml, context, MockCryptoSystem())

        assert(actual).isEqualTo(expectedTx)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseGTXMLTransaction_in_context_with_not_found_params_throws_exception() {
        val xml = javaClass.getResource("/net/postchain/gtx/gtxml/parse/tx_full_params_not_found.xml").readText()

        GTXMLTransactionParser.parseGTXMLTransaction(
                xml, TransactionContext.empty(), MockCryptoSystem())
    }

    @Test(expected = UserMistake::class)
    fun parseGTXMLTransaction_in_context_with_not_bytea_param_in_signers_throws_exception() {
        val xml = javaClass.getResource("/net/postchain/gtx/gtxml/parse/tx_params_not_bytea_signer.xml").readText()

        val context = TransactionContext(
                null,
                mapOf(
                        "param_foo" to GtvString("my string param")
                )
        )

        GTXMLTransactionParser.parseGTXMLTransaction(xml, context, MockCryptoSystem())
    }

    @Test(expected = UserMistake::class)
    fun parseGTXMLTransaction_in_context_with_not_bytea_param_in_signature_throws_exception() {
        val xml = javaClass.getResource("/net/postchain/gtx/gtxml/parse/tx_params_not_bytea_signature.xml").readText()

        val context = TransactionContext(
                null,
                mapOf(
                        "param_foo" to GtvString("my string param")
                )
        )

        GTXMLTransactionParser.parseGTXMLTransaction(xml, context, MockCryptoSystem())
    }

    @Test
    fun parseGTXMLTransaction_in_context_with_compound_parameters_of_operation_successfully() {
        val xml = javaClass.getResource(
                "/net/postchain/gtx/gtxml/parse/tx_params_is_compound_of_parameter_of_operation.xml").readText()

        val expectedBody = GTXTransactionBodyData(
                blockchainRID,
                arrayOf(
                        OpData("ft_transfer",
                                arrayOf(GtvArray(arrayOf(
                                        GtvString("foo"),
                                        GtvArray(arrayOf(
                                                GtvString("foo"),
                                                GtvString("bar")
                                        )),
                                        GtvDictionary.build(mapOf(
                                                "key2" to GtvString("42"),
                                                "key1" to GtvInteger(42),
                                                "key3" to GtvArray(arrayOf(
                                                        GtvString("hello"),
                                                        GtvInteger(42)))
                                        ))
                                )))
                        )
                ),
                arrayOf()
        )

        val expectedTx = GTXTransactionData( expectedBody, arrayOf())

        val context = TransactionContext(
                null,
                mapOf("param_compound" to
                        GtvArray(arrayOf(
                                GtvString("foo"),
                                GtvArray(arrayOf(
                                        GtvString("foo"),
                                        GtvString("bar")
                                )),
                                GtvDictionary.build(mapOf(
                                        "key1" to GtvInteger(42),
                                        "key2" to GtvString("42"),
                                        "key3" to GtvArray(arrayOf(
                                                GtvString("hello"),
                                                GtvInteger(42)
                                        ))
                                ))
                        ))
                )
        )

        val actual = GTXMLTransactionParser.parseGTXMLTransaction(xml, context, MockCryptoSystem())

        assert(actual).isEqualTo(expectedTx)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseGTXMLTransaction_signers_more_than_signatures_throws_exception() {
        val xml = javaClass.getResource(
                "/net/postchain/gtx/gtxml/parse/tx_signers_and_signatures_incompatibility__signers_more_than_signatures.xml")
                .readText()

        GTXMLTransactionParser.parseGTXMLTransaction(xml, TransactionContext.empty(), MockCryptoSystem())
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseGTXMLTransaction_signers_less_than_signatures_throws_exception() {
        val xml = javaClass.getResource(
                "/net/postchain/gtx/gtxml/parse/tx_signers_and_signatures_incompatibility__signers_less_than_signatures.xml")
                .readText()

        GTXMLTransactionParser.parseGTXMLTransaction(xml, TransactionContext.empty(), MockCryptoSystem())
    }

    @Test
    fun parseGTXMLTransaction_no_signatures_element_successfully() {
        val xml = javaClass.getResource(
                "/net/postchain/gtx/gtxml/parse/tx_signers_and_signatures_incompatibility__no_signatures_element.xml")
                .readText()

        val expectedBody = GTXTransactionBodyData(
                blockchainRID,
                arrayOf(),
                arrayOf(
                        byteArrayOf(0x12, 0x38, 0x71, 0x23),
                        byteArrayOf(0x12, 0x38, 0x71, 0x24),
                        byteArrayOf(0x12, 0x38, 0x71, 0x25)
                )
        )

        val expectedTx = GTXTransactionData(expectedBody,
                arrayOf(
                        byteArrayOf(),
                        byteArrayOf(),
                        byteArrayOf()
                )
        )

        val actual = GTXMLTransactionParser.parseGTXMLTransaction(xml, TransactionContext.empty(), MockCryptoSystem())

        assert(actual).isEqualTo(expectedTx)
    }
}