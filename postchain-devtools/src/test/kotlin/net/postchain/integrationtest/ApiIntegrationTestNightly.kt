// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.integrationtest

import io.restassured.RestAssured.given
import net.postchain.base.BaseBlockHeader
import net.postchain.base.MerklePath
import net.postchain.base.MerklePathItem
import net.postchain.base.Side
import net.postchain.common.RestTools
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.core.Signature
import net.postchain.core.Transaction
import net.postchain.integrationtest.JsonTools.jsonAsMap
import net.postchain.devtools.EbftIntegrationTest
import net.postchain.devtools.testinfra.TestTransaction
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test


@Ignore
class ApiIntegrationTestNightly : EbftIntegrationTest() {

    private val gson = JsonTools.buildGson()
    private val blockchainRID = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3"
    private var txHashHex = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

    @Test
    fun testMixedAPICalls() {
        createEbftNodes(3)

        testStatusGet("/tx/$blockchainRID/$txHashHex", 404)
        testStatusGet("/tx/$blockchainRID/$txHashHex/status", 200) {
            assertEquals(
                    jsonAsMap(gson, "{\"status\"=\"unknown\"}"),
                    jsonAsMap(gson, it))
        }

        val tx = TestTransaction(1)
        testStatusPost(
                0,
                "/tx/$blockchainRID",
                "{\"tx\": \"${tx.getRawData().toHex()}\"}",
                200)

        awaitConfirmed(blockchainRID, tx)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun testConfirmationProof() {
        val nodeCount = 3
        createEbftNodes(nodeCount)

        var blockHeight = 0
        var currentId = 0

        for (txCount in 1..16) {

            for (i in 1..txCount) {
                val tx = TestTransaction(++currentId)
                testStatusPost(
                        blockHeight % nodeCount, "/tx/$blockchainRID",
                        "{\"tx\": \"${tx.getRawData().toHex()}\"}",
                        200)
            }

            awaitConfirmed(blockchainRID, TestTransaction(currentId))
            blockHeight++

            for (i in 0 until txCount) {
                val tx = TestTransaction(currentId - i)

                val body = given().port(nodes[0].getRestApiHttpPort())
                        .get("/tx/$blockchainRID/${tx.getRID().toHex()}/confirmationProof")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body().asString()

                val actualMap = jsonAsMap(gson, body)

                // Assert tx hash
                val hash = (actualMap["hash"] as String).hexStringToByteArray()
                assertArrayEquals(tx.getHash(), hash)

                // Assert signatures
                val verifier = cryptoSystem.makeVerifier()
                val blockHeader = (actualMap["blockHeader"] as String).hexStringToByteArray()
                val signatures = actualMap["signatures"] as List<Map<String, String>>
                signatures.forEach {
                    assertTrue(verifier(blockHeader,
                            Signature(it["pubKey"]!!.hexStringToByteArray(),
                                    it["signature"]!!.hexStringToByteArray())))
                }

                // Build MerklePath
                val path = MerklePath()
                val merklePath = actualMap["merklePath"] as List<Map<String, Any>>
                merklePath.forEach {
                    // The use of 0.0 and 1.0 is to work around that the json parser creates doubles
                    // instances from json integer values
                    val side = when {
                        it["side"] == 0.0 -> Side.LEFT
                        it["side"] == 1.0 -> Side.RIGHT
                        else -> throw AssertionError("Invalid 'side' of merkle path: ${it["side"]}")
                    }
                    val pathItemHash = (it["hash"] as String).hexStringToByteArray()
                    path.add(MerklePathItem(side, pathItemHash))
                }

                // Assert Merkle Path
                val header = nodes[0]
                        .getBlockchainInstance()
                        .blockchainConfiguration
                        .decodeBlockHeader(blockHeader) as BaseBlockHeader
                assertTrue(header.validateMerklePath(path, tx.getHash()))
            }
        }
    }

    private fun awaitConfirmed(blockchainRID: String, tx: Transaction) {
        RestTools.awaitConfirmed(
                nodes[0].getRestApiHttpPort(),
                blockchainRID,
                tx.getRID().toHex())
    }

    private fun testStatusGet(path: String, expectedStatus: Int, extraChecks: (responseBody: String) -> Unit = {}) {
        val response = given().port(nodes[0].getRestApiHttpPort())
                .get(path)
                .then()
                .statusCode(expectedStatus)
                .extract()

        extraChecks(response.body().asString())
    }

    private fun testStatusPost(toIndex: Int, path: String, body: String, expectedStatus: Int) {
        given().port(nodes[toIndex].getRestApiHttpPort())
                .body(body)
                .post(path)
                .then()
                .statusCode(expectedStatus)
    }
}