// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.api.rest

import io.restassured.RestAssured.given
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.common.RestTools
import net.postchain.common.toHex
import net.postchain.configurations.GTXTestModule
import net.postchain.gtx.EMPTY_SIGNATURE
import net.postchain.gtx.GTXDataBuilder
import net.postchain.gtx.GTXTransactionFactory
import net.postchain.gtx.gtx
import net.postchain.test.KeyPairHelper.privKey
import net.postchain.test.KeyPairHelper.pubKey
import org.hamcrest.core.IsEqual.equalTo
import java.util.*

class RestApiTestManual {
    private val port = 58373
    private val cryptoSystem = SECP256K1CryptoSystem()
    private val blockchainRID = "78967BAA4768CBCEF11C508326FFB13A956689FCB6DC3BA17F4B895CBB1577A3"

    //    @Test
    fun testGtxTestModuleBackend() {
        org.awaitility.Awaitility.await()

        val query = """{"type"="gtx_test_get_value", "txRID"="abcd"}"""
        given().port(port)
                .body(query)
                .post("/query")
                .then()
                .statusCode(200)
                .body(equalTo("null"))

        val txBytes = buildTestTx(1L, "hello${Random().nextLong()}")
        given().port(port)
                .body("""{"tx"="${txBytes.toHex()}"}""")
                .post("/tx")
                .then()
                .statusCode(200)

        val transaction = GTXTransactionFactory(EMPTY_SIGNATURE, GTXTestModule(), cryptoSystem)
                .decodeTransaction(txBytes)
        RestTools.awaitConfirmed(port, blockchainRID, transaction.getRID().toHex())
    }

    private fun buildTestTx(id: Long, value: String): ByteArray {
        val b = GTXDataBuilder(EMPTY_SIGNATURE, arrayOf(pubKey(0)), cryptoSystem)
        b.addOperation("gtx_test", arrayOf(gtx(id), gtx(value)))
        b.finish()
        b.sign(cryptoSystem.makeSigner(pubKey(0), privKey(0)))
        return b.serialize()
    }
}