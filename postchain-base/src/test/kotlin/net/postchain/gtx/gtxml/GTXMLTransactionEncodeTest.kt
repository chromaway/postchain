package net.postchain.gtx.gtxml

import assertk.assert
import assertk.assertions.isEqualTo
import net.postchain.common.hexStringToByteArray
import net.postchain.gtx.*
import org.junit.Test
import net.postchain.gtv.*

class GTXMLTransactionEncodeTest {

    @Test
    fun encodeXMLGTXTransaction_successfully() {
        val gtxData = GTXData(
                "23213213".hexStringToByteArray(),
                arrayOf(
                        byteArrayOf(0x12, 0x38, 0x71, 0x23),
                        byteArrayOf(0x12, 0x38, 0x71, 0x24)
                ),
                arrayOf(
                        byteArrayOf(0x34, 0x56, 0x78, 0x54),
                        byteArrayOf(0x34, 0x56, 0x78, 0x55)
                ),
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
                )
        )

        val expected = javaClass.getResource("/net/postchain/gtx/gtxml/encode/tx_full.xml").readText()
                .replace("\r\n", "\n").trim()
        val actual = GTXMLTransactionEncoder.encodeXMLGTXTransaction(gtxData)

        assert(actual.trim()).isEqualTo(expected)
    }

    @Test
    fun encodeXMLGTXTransaction_empty_successfully() {
        val gtxData = GTXData(
                "23213213".hexStringToByteArray(),
                arrayOf(),
                arrayOf(),
                arrayOf()
        )

        val expected = javaClass.getResource("/net/postchain/gtx/gtxml/encode/tx_empty.xml").readText()
                .replace("\r\n", "\n").trim()
        val actual = GTXMLTransactionEncoder.encodeXMLGTXTransaction(gtxData)

        assert(actual.trim()).isEqualTo(expected)
    }

    @Test
    fun encodeXMLGTXTransaction_with_empty_signers_and_signatures_successfully() {
        val gtxData = GTXData(
                "23213213".hexStringToByteArray(),
                arrayOf(),
                arrayOf(),
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
                )
        )

        val expected = javaClass.getResource("/net/postchain/gtx/gtxml/encode/tx_empty_signers_and_signatures.xml").readText()
                .replace("\r\n", "\n").trim()
        val actual = GTXMLTransactionEncoder.encodeXMLGTXTransaction(gtxData)

        assert(actual.trim()).isEqualTo(expected)
    }

    @Test
    fun encodeXMLGTXTransaction_with_empty_operations_successfully() {
        val gtxData = GTXData(
                "23213213".hexStringToByteArray(),
                arrayOf(
                        byteArrayOf(0x12, 0x38, 0x71, 0x23),
                        byteArrayOf(0x12, 0x38, 0x71, 0x24)
                ),
                arrayOf(
                        byteArrayOf(0x34, 0x56, 0x78, 0x54),
                        byteArrayOf(0x34, 0x56, 0x78, 0x55)
                ),
                arrayOf()
        )

        val expected = javaClass.getResource("/net/postchain/gtx/gtxml/encode/tx_empty_operations.xml").readText()
                .replace("\r\n", "\n").trim()
        val actual = GTXMLTransactionEncoder.encodeXMLGTXTransaction(gtxData)

        assert(actual.trim()).isEqualTo(expected)
    }

    @Test
    fun encodeXMLGTXTransaction_with_empty_operation_parameters_successfully() {
        val gtxData = GTXData(
                "23213213".hexStringToByteArray(),
                arrayOf(
                        byteArrayOf(0x12, 0x38, 0x71, 0x23),
                        byteArrayOf(0x12, 0x38, 0x71, 0x24)
                ),
                arrayOf(
                        byteArrayOf(0x34, 0x56, 0x78, 0x54),
                        byteArrayOf(0x34, 0x56, 0x78, 0x55)
                ),
                arrayOf(
                        OpData("ft_transfer", arrayOf()),
                        OpData("ft_transfer", arrayOf())
                )
        )

        val expected = javaClass.getResource("/net/postchain/gtx/gtxml/encode/tx_empty_operation_parameters.xml").readText()
                .replace("\r\n", "\n").trim()
        val actual = GTXMLTransactionEncoder.encodeXMLGTXTransaction(gtxData)

        assert(actual.trim()).isEqualTo(expected)
    }

    @Test
    fun encodeXMLGTXTransaction_compound_parameter_of_operation_successfully() {
        val gtxData = GTXData(
                "23213213".hexStringToByteArray(),
                arrayOf(),
                arrayOf(),
                arrayOf(
                        OpData("ft_transfer",
                                arrayOf(
                                        GtvString("foo"),
                                        GtvArray(arrayOf(
                                                GtvString("foo"),
                                                GtvArray(arrayOf(
                                                        GtvString("foo"),
                                                        GtvString("bar")
                                                )),
                                                GtvDictionary(mapOf(
                                                        "key1" to GtvInteger(42),
                                                        "key2" to GtvString("42"),
                                                        "key3" to GtvArray(arrayOf(
                                                                GtvString("hello"),
                                                                GtvInteger(42)
                                                        ))
                                                ))
                                        )),
                                        GtvDictionary(mapOf(
                                                "key1" to GtvNull,
                                                "key2" to GtvString("42")
                                        ))
                                ))
                ))

        val expected = javaClass.getResource("/net/postchain/gtx/gtxml/encode/tx_compound_parameter_of_operation.xml").readText()
                .replace("\r\n", "\n").trim()
        val actual = GTXMLTransactionEncoder.encodeXMLGTXTransaction(gtxData)

        assert(actual.trim()).isEqualTo(expected)
    }
}