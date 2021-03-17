// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.api.rest

import io.restassured.RestAssured.given
import net.postchain.base.BlockchainRid
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.common.RestTools
import net.postchain.common.toHex
import net.postchain.configurations.GTXTestModule
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GTXDataBuilder
import net.postchain.gtx.GTXTransactionFactory
import org.hamcrest.core.IsEqual.equalTo
import java.util.*

class RestApiTestManual {
    private val port = 58373
    private val cryptoSystem = SECP256K1CryptoSystem()
    // TODO Olle POS-93 where do we get it?
    private var blockchainRID: BlockchainRid? = null //"78967BAA4768CBCEF11C508326FFB13A956689FCB6DC3BA17F4B895CBB1577A3"

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

        val transaction = GTXTransactionFactory(BlockchainRid.ZERO_RID, GTXTestModule(), cryptoSystem)
                .decodeTransaction(txBytes)
        RestTools.awaitConfirmed(port, blockchainRID!!.toHex(), transaction.getRID().toHex())
    }

    private fun buildTestTx(id: Long, value: String): ByteArray {
        val b = GTXDataBuilder(BlockchainRid.ZERO_RID, arrayOf(pubKey(0)), cryptoSystem)
        b.addOperation("gtx_test", arrayOf(gtv(id), gtv(value)))
        b.finish()
        b.sign(cryptoSystem.buildSigMaker(pubKey(0), privKey(0)))
        return b.serialize()
    }
}