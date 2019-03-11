package net.postchain.gtx.gtxml

import assertk.assert
import assertk.assertions.isEqualTo
import net.postchain.common.hexStringToByteArray
import net.postchain.core.byteArrayKeyOf
import net.postchain.gtv.GtvInteger
import net.postchain.gtx.OpData
import net.postchain.gtv.GtvString
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.devtools.MockCryptoSystem
import net.postchain.gtx.GTXTransactionBodyData
import net.postchain.gtx.GTXTransactionData
import org.junit.Test

class GTXMLTransactionParserAutoSignTest {

    @Test
    fun autoSign_autosigning_for_empty_signatures_successfully() {
        val xml = readResourceFile("tx_two_empty_signatures.xml")

        val pubKey0 = pubKey(0)
        val privKey0 = privKey(0)
        val signer0 = MockCryptoSystem().makeSigner(pubKey0, privKey0)

        val pubKey1 = pubKey(1)
        val privKey1 = privKey(1)
        val signer1 = MockCryptoSystem().makeSigner(pubKey1, privKey1)

        val expectedBody = GTXTransactionBodyData(
                "23213213".hexStringToByteArray(),
                arrayOf(
                        OpData("ft_transfer",
                                arrayOf(
                                        GtvString("hello"),
                                        GtvString("hello2"),
                                        GtvString("hello3"),
                                        GtvInteger(42),
                                        GtvInteger(43)))
                ),
                arrayOf(
                        pubKey0,
                        byteArrayOf(0x12, 0x38, 0x71, 0x23),
                        byteArrayOf(0x12, 0x38, 0x71, 0x24),
                        pubKey1
                )
        )

        val expectedTx = GTXTransactionData( expectedBody,
                arrayOf(
                        byteArrayOf(), // empty signature, will calculated below
                        byteArrayOf(0x34, 0x56, 0x78, 0x54),
                        byteArrayOf(0x34, 0x56, 0x78, 0x55),
                        byteArrayOf() // empty signature, will calculated below
                )
        )

        // Auto-signing
        expectedTx.signatures[0] = signer0(expectedBody.serialize()).data
        expectedTx.signatures[3] = signer1(expectedBody.serialize()).data

        val actual = GTXMLTransactionParser.parseGTXMLTransaction(
                xml,
                TransactionContext(
                        null,
                        mapOf(),
                        true,
                        mapOf(
                                pubKey0.byteArrayKeyOf() to signer0,
                                pubKey1.byteArrayKeyOf() to signer1)
                )
        )

        assert(actual).isEqualTo(expectedTx)
    }

    @Test(expected = IllegalArgumentException::class)
    fun autoSign_autosigning_no_signer_throws_exception() {
        val xml = readResourceFile("tx_no_signer.xml")

        GTXMLTransactionParser.parseGTXMLTransaction(xml,
                TransactionContext(null, mapOf(), true, mapOf()))
    }

    @Test
    fun autoSign_no_autosigning_and_no_signer_successfully() {
        val xml = readResourceFile("tx_two_empty_signatures.xml")

        val pubKey0 = pubKey(0)
        val pubKey1 = pubKey(1)

        val expectedBody = GTXTransactionBodyData(
                "23213213".hexStringToByteArray(),
                arrayOf(
                        OpData("ft_transfer",
                                arrayOf(
                                        GtvString("hello"),
                                        GtvString("hello2"),
                                        GtvString("hello3"),
                                        GtvInteger(42),
                                        GtvInteger(43)))
                ),
                arrayOf(
                        pubKey0,
                        byteArrayOf(0x12, 0x38, 0x71, 0x23),
                        byteArrayOf(0x12, 0x38, 0x71, 0x24),
                        pubKey1
                )
        )

        val expectedTx = GTXTransactionData(
                expectedBody,
                arrayOf(
                        byteArrayOf(), // empty signature
                        byteArrayOf(0x34, 0x56, 0x78, 0x54),
                        byteArrayOf(0x34, 0x56, 0x78, 0x55),
                        byteArrayOf() // empty signature
                )
        )

        val actual = GTXMLTransactionParser.parseGTXMLTransaction(xml,
                TransactionContext(null, mapOf(), false, mapOf()))

        assert(actual).isEqualTo(expectedTx)
    }

    @Test
    fun autoSign_autosigning_no_signatures_element_successfully() {
        val xml = readResourceFile("tx_no_signatures_element.xml")

        val pubKey0 = pubKey(0)
        val privKey0 = privKey(0)
        val signer0 = MockCryptoSystem().makeSigner(pubKey0, privKey0)

        val pubKey1 = pubKey(1)
        val privKey1 = privKey(1)
        val signer1 = MockCryptoSystem().makeSigner(pubKey1, privKey1)

        val expectedBody = GTXTransactionBodyData(
                "23213213".hexStringToByteArray(),
                arrayOf(
                        OpData("ft_transfer",
                                arrayOf(
                                        GtvString("hello"),
                                        GtvString("hello2"),
                                        GtvString("hello3"),
                                        GtvInteger(42),
                                        GtvInteger(43)))
                ),
                arrayOf(pubKey0, pubKey1)
        )

        val expectedTx = GTXTransactionData(expectedBody,
                arrayOf(byteArrayOf(), byteArrayOf()) // empty signatures, will calculated below
                )

        // Auto-signing
        expectedTx.signatures[0] = signer0(expectedBody.serialize()).data
        expectedTx.signatures[1] = signer1(expectedBody.serialize()).data

        val actual = GTXMLTransactionParser.parseGTXMLTransaction(
                xml,
                TransactionContext(
                        null,
                        mapOf(),
                        true,
                        mapOf(
                                pubKey0.byteArrayKeyOf() to signer0,
                                pubKey1.byteArrayKeyOf() to signer1
                        )
                )
        )

        assert(actual).isEqualTo(expectedTx)
    }

    private fun readResourceFile(filename: String): String {
        return javaClass.getResource("/net/postchain/gtx/gtxml/auto-sign/$filename").readText()
    }
}