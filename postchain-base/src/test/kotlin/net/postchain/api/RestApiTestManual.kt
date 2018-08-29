// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.api

import io.restassured.RestAssured.given
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.common.RestTools
import net.postchain.common.toHex
import net.postchain.configurations.GTXTestModule
import net.postchain.gtx.*
import org.hamcrest.core.IsEqual.equalTo
import java.util.*

class RestApiTestManual {
    private val port = 58373
    private val cryptoSystem = SECP256K1CryptoSystem()

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
        RestTools.awaitConfirmed(port, transaction.getRID().toHex())
    }

    private fun buildTestTx(id: Long, value: String): ByteArray {
        val b = GTXDataBuilder(EMPTY_SIGNATURE, arrayOf(pubKey(0)), cryptoSystem)
        b.addOperation("gtx_test", arrayOf(gtx(id), gtx(value)))
        b.finish()
        b.sign(cryptoSystem.makeSigner(pubKey(0), privKey(0)))
        return b.serialize()
    }
}