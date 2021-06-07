// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest.api

import io.restassured.RestAssured.given
import net.postchain.base.BaseBlockHeader
import net.postchain.base.BlockchainRid
import net.postchain.base.merkle.Hash
import net.postchain.common.RestTools
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.configurations.GTXTestModule
import net.postchain.core.Signature
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.KeyPairHelper
import net.postchain.devtools.testinfra.TestOneOpGtxTransaction
import net.postchain.devtools.utils.configuration.SystemSetup
import net.postchain.devtools.utils.configuration.system.SystemSetupFactory
import net.postchain.gtv.*
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
import net.postchain.gtv.merkle.proof.GtvMerkleProofTreeFactory
import net.postchain.gtv.merkle.proof.merkleHash
import net.postchain.gtx.GTXDataBuilder
import net.postchain.gtx.GTXTransactionFactory
import net.postchain.integrationtest.JsonTools
import net.postchain.integrationtest.JsonTools.jsonAsMap
import org.awaitility.Awaitility
import org.hamcrest.core.IsEqual
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiIntegrationTestNightly : IntegrationTestSetup() {

    private val gson = JsonTools.buildGson()
    private var txHashHex = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

    private val gtxTestModule = GTXTestModule()
    private val gtxTextModuleOperation = "gtx_test" // This is a real operation
    private val chainIid = 1


    private fun doSystemSetup(nodeCount: Int, bcConfFileName: String): SystemSetup {
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodeCount))
        val bcConfFileMap = mapOf(chainIid to bcConfFileName)
        val sysSetup = SystemSetupFactory.buildSystemSetup(bcConfFileMap)
        assertEquals(nodeCount, sysSetup.nodeMap.size, "We didn't get the nodes we expected, check BC config file")
        sysSetup.needRestApi = true // NOTE!! This is important in this test!!

        createNodesFromSystemSetup(sysSetup)
        return sysSetup
    }

    @Test
    fun testMixedAPICalls() {
        val nodeCount = 3
        val sysSetup = doSystemSetup(nodeCount,"/net/postchain/devtools/api/blockchain_config.xml")
        val blockchainRIDBytes = sysSetup.blockchainMap[chainIid]!!.rid
        val blockchainRID = blockchainRIDBytes.toHex()

        testStatusGet("/tx/$blockchainRID/$txHashHex", 404)
        testStatusGet("/tx/$blockchainRID/$txHashHex/status", 200) {
            assertEquals(
                    jsonAsMap(gson, "{\"status\"=\"unknown\"}"),
                    jsonAsMap(gson, it))
        }

        val factory = GTXTransactionFactory(blockchainRIDBytes, gtxTestModule, cryptoSystem)


        val blockHeight = 0 // If we set it to zero the node with index 0 will get the post
        val tx = postGtxTransaction(factory, 1, blockHeight, nodeCount, blockchainRIDBytes)

        awaitConfirmed(blockchainRID, tx!!.getRID())

        // Note: here we use the "iid_1" method instead of BC RID
        testStatusGet("/tx/iid_${chainIid.toInt().toString()}/${tx!!.getRID().toHex()}/status", 200) {
            assertEquals(
                    jsonAsMap(gson, "{\"status\"=\"confirmed\"}"),
                    jsonAsMap(gson, it))
        }
    }

    @Test
    fun testDirectQueryApi() {
        val nodeCount = 1

        val sysSetup = doSystemSetup(nodeCount, "/net/postchain/devtools/api/blockchain_config_dquery.xml")
        val blockchainRIDBytes = sysSetup.blockchainMap[chainIid]!!.rid
        val blockchainRID = blockchainRIDBytes.toHex()

        buildBlockAndCommit(nodes[0])

        // /get_picture
        val expect1 = "abcd"
        val byteArray = given().port(nodes[0].getRestApiHttpPort())
                .get("/dquery/$blockchainRID?type=get_picture&id=1234")
                .then()
                .statusCode(200)
                .extract().asByteArray()
        assertEquals(expect1, String(byteArray))

        // /get_front_page
        val expect2 = "<h1>it works!</h1>"
        val text = given().port(nodes[0].getRestApiHttpPort())
                .get("/dquery/$blockchainRID?type=get_front_page&id=1234")
                .then()
                .statusCode(200)
                .extract().asString()
        assertEquals(expect2, text)
    }

    @Test
    fun testGetQuery() {
        val nodesCount = 1
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        configOverrides.setProperty("api.port", 0)
        val nodes = createNodes(nodesCount, "/net/postchain/devtools/api/blockchain_config_getquery.xml")
        val blockchainRIDBytes = nodes[0].getBlockchainRid(1L)!! // Just take first chain from first node.
        val blockchainRID = blockchainRIDBytes.toHex()

        buildBlockAndCommit(nodes[0])

        // returns `num * num`
        val num = 1000
        val expect1 = "1000000"
        var returnVal = given().port(nodes[0].getRestApiHttpPort())
                .get("/query/$blockchainRID?type=test_query&i=$num&flag=true")
                .then()
                .statusCode(200)
                .extract().asString()
        assertEquals(expect1, returnVal)

        // returns `num`
        val expect2 = "1000"
        returnVal = given().port(nodes[0].getRestApiHttpPort())
                .get("/query/$blockchainRID?type=test_query&i=$num&flag=false")
                .then()
                .statusCode(200)
                .extract().asString()
        assertEquals(expect2, returnVal)
    }

    @Test
    fun testBatchQueriesApi() {
        val nodesCount = 1
        val blocksCount = 1
        val txPerBlock = 1

        val sysSetup = doSystemSetup(nodesCount, "/net/postchain/devtools/api/blockchain_config_1.xml")
        val blockchainRIDBytes = sysSetup.blockchainMap[chainIid]!!.rid
        val blockchainRID = blockchainRIDBytes.toHex()

        buildBlockAndCommit(nodes[0])
        val query = """{"queries": [{"type"="gtx_test_get_value", "txRID"="abcd"},
                                    {"type"="gtx_test_get_value", "txRID"="cdef"}]}""".trimMargin()
        given().port(nodes[0].getRestApiHttpPort())
                .body(query)
                .post("/batch_query/$blockchainRID")
                .then()
                .statusCode(200)
                .body(IsEqual.equalTo("[\"null\",\"null\"]"))
    }

    @Test
    fun testQueryGTXApi() {
        val nodesCount = 1
        val blocksCount = 1
        val txPerBlock = 1

        val sysSetup = doSystemSetup(nodesCount,"/net/postchain/devtools/api/blockchain_config_1.xml")
        val blockchainRIDBytes = sysSetup.blockchainMap[chainIid]!!.rid
        val blockchainRID = blockchainRIDBytes.toHex()

        buildBlockAndCommit(nodes[0])

        val gtxQuery1 = gtv(gtv("gtx_test_get_value"), gtv("txRID" to gtv("abcd")))
        val gtxQuery2 = gtv(gtv("gtx_test_get_value"), gtv("txRID" to gtv("cdef")))
        val jsonQuery = """{"queries" : ["${GtvEncoder.encodeGtv(gtxQuery1).toHex()}", "${GtvEncoder.encodeGtv(gtxQuery2).toHex()}"]}""".trimMargin()


        val response = given().port(nodes[0].getRestApiHttpPort())
                .body(jsonQuery)
                .post("/query_gtx/$blockchainRID")
                .then()
                .statusCode(200)
                .body(IsEqual.equalTo("[\"A0020500\",\"A0020500\"]"))

    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun testConfirmationProof() {
        val nodeCount = 3

        val sysSetup = doSystemSetup(nodeCount,"/net/postchain/devtools/api/blockchain_config.xml")
        val blockchainRIDBytes = sysSetup.blockchainMap[chainIid]!!.rid
        val blockchainRID = blockchainRIDBytes.toHex()

        val factory = GTXTransactionFactory(blockchainRIDBytes, gtxTestModule, cryptoSystem)

        var blockHeight = 0
        var currentId = 0

        for (txCount in 1..16) {

            println("----------------- Running testConfirmationProof with txCount: $txCount ---------------------")
            val txList = mutableListOf<TestOneOpGtxTransaction>()
            for (i in 1..txCount) {
                txList.add(postGtxTransaction(factory, ++currentId, blockHeight, nodeCount, blockchainRIDBytes))
            }

            txList.forEach {
                // Wait for all txs to confirm. They are posted to different nodes and all
                // txs might not arrive at all nodes prior to block building. It's therefore not
                // enough to await last tx being confirmed on node 0.
                awaitConfirmed(blockchainRID, it.getRID())
            }

            txList.reverse() // We begin with the last TX that we saved from last step
            val txArr = txList.toTypedArray()

            blockHeight++

            for (i in 0 until txCount) {
                val realTx = txArr[i]!!
                val jsonResponse = fetchConfirmationProof(realTx, i, blockchainRIDBytes)
                checkConfirmationProofForTx(realTx, jsonResponse)
            }
        }
    }

    @Test
    fun testRejectedTransactionWithReason() {
        val nodesCount = 1
        val sysSetup = doSystemSetup(nodesCount,"/net/postchain/devtools/api/blockchain_config_rejected.xml")
        val bcRid = sysSetup.blockchainMap[chainIid]!!.rid
        val blockchainRID = bcRid.toHex()

        val builder = createBuilder(bcRid, "rejectMe")

        // post transaction
        testStatusPost(
                0,
                "/tx/$blockchainRID",
                "{\"tx\": \"${builder.serialize().toHex()}\"}",
                200)

        // Asserting
        val txRidHex = builder.getDigestForSigning().toHex()
        val expected = """
            {
                "status": "rejected",
                "rejectReason": "You were asking for it"
            }
        """.trimIndent()

        Awaitility.await().untilAsserted {
            val body = given().port(nodes[0].getRestApiHttpPort())
                    .get("/tx/$blockchainRID/$txRidHex/status")
                    .then()
                    .statusCode(200)
                    .extract().body().asString()

            JSONAssert.assertEquals(expected, body, JSONCompareMode.STRICT)
        }
    }

    /**
     * Will create and post a transaction to the servers
     *
     * @return the posted transaction
     */
    private fun postGtxTransaction(
            factory: GTXTransactionFactory,
            currentId: Int,
            blockHeight: Int,
            nodeCount: Int,
            bcRid: BlockchainRid
    ): TestOneOpGtxTransaction {

        val tx = TestOneOpGtxTransaction(factory, currentId)
        val strHexData = tx.getRawData().toHex()
        //println("Sending TX: $strHexData:")
        testStatusPost(
                blockHeight % nodeCount,
                "/tx/${bcRid.toHex()}",
                "{\"tx\": \"$strHexData\"}",
                200)

        return tx
    }

    /**
     * Fetch the confirmation proof from the server for the given TX.
     *
     * An example of what the JSON response might look like:
     *
     *  {
     *    "hash":"93A4..0F",
     *    "blockHeader":"A581..00",
     *    "signatures":[
     *      {
     *        "pubKey":"03A3..70",
     *        "signature":"3CE..F3"
     *      },
     *      {
     *        "pubKey":"031B..8F",
     *        "signature":"D5F3..E0"
     *      },
     *      {
     *        "pubKey":"03B2..94"
     *        ,"signature":"33C8..3E"
     *      }
     *    ],
     *    "merkleProofTree":[
     *      103,
     *      1,
     *      -10,
     *      [
     *        101,
     *        0,
     *        "93A4..0F"
     *      ],
     *      [
     *        100,
     *        "0000..00"
     *      ]
     *     ]
     *  }
     *
     *
     * @param realTx is the transaction we need to prove.
     * @param seqNr is just for debugging
     * @return the Json converted to a [Map]
     */
    private fun fetchConfirmationProof(realTx: TestOneOpGtxTransaction, seqNr: Int, bcRid: BlockchainRid): String {
        val txRidHex = realTx.getRID().toHex()
        println("Fetching conf proof for tx nr: $seqNr with tx RID: $txRidHex ")
        val body = given().port(nodes[0].getRestApiHttpPort())
                .get("/tx/${bcRid.toHex()}/${txRidHex}/confirmationProof")
                .then()
                .statusCode(200)
                .extract()
                .body().asString()

        println("Response: $body")

        return body
    }

    /**
     * Verify that the transaction is in the block, and verify that the confirmation proof is correct. It should have:
     *   2.a hash
     *   2.b signatures
     *   2.c merkle path
     *
     * @param realTx - the transaction to check
     * @param actualMap - a map of json parts that is the proof
     */
    private fun checkConfirmationProofForTx(realTx: TestOneOpGtxTransaction, jsonBody: String) {

        val actualMap: Map<String, Any> = jsonAsMap(gson, jsonBody)

        // Assert tx hash
        val hash = (actualMap["hash"] as String).hexStringToByteArray()
        assertArrayEquals(realTx.getHash(), hash)

        // Assert signatures
        val blockHeaderRaw = (actualMap["blockHeader"] as String).hexStringToByteArray()
        val blockHeader = BaseBlockHeader(blockHeaderRaw, cryptoSystem)
        val blockRid = blockHeader.blockRID

        val signatures = actualMap["signatures"] as List<Map<String, String>>
        signatures.forEach {
            val signature = Signature(it["pubKey"]!!.hexStringToByteArray(), it["signature"]!!.hexStringToByteArray())
            assertTrue(cryptoSystem.verifyDigest(blockRid, signature))
        }

        val blockMerkleRootHashFromHeader = blockHeader.blockHeaderRec.getMerkleRootHash()
        println("blockMerkleRootHash - from header: ${blockMerkleRootHashFromHeader.toHex()}")
        // -------------------
        // Merkle Proof Tree
        // -------------------

        // a) Do we have the value to prove
        val merkleProofTree = actualMap["merkleProofTree"] as List<Any>
        val found = GtvProofTreeTestHelper.findHashInBlockProof(realTx.getHash(), merkleProofTree)
        assertTrue(found, "The proof does not contain the hash we expected")

        // b) Calculate the merkle root of the proof
        // JSON -> GTV
        val gsonGtv = make_gtv_gson()
        val gtvDictBody = gsonGtv.fromJson<Gtv>(jsonBody, Gtv::class.java)
        val gtvProof = gtvDictBody!!.asDict().get("merkleProofTree")

        // TODO: Should this really be done? Shouldn't we make the JSON format understand Binary?
        val gtvCleanProof = GtvProofTreeTestHelper.translateGtvStringToGtvByteArray(gtvProof!!)
        println("Proof as gtv: $gtvCleanProof")
        // GTV -> Proof
        val proofTreeFactory = GtvMerkleProofTreeFactory()
        val x = proofTreeFactory.deserialize(gtvCleanProof as GtvArray)
        println("Proof as classes: $x")
        val myNewBlockHash = x.merkleHash(GtvMerkleHashCalculator(cryptoSystem))

        // Assert we get the same block RID
        println("Block merkle root - calculated : ${myNewBlockHash.toHex()}")
        assertTrue(myNewBlockHash.contentEquals(blockMerkleRootHashFromHeader),
                "The block merkle root calculated from the proof doesn't correspond to the block's merkle root hash from the header")

    }

    private fun awaitConfirmed(blockchainRID: String, txRid: Hash) {
        RestTools.awaitConfirmed(
                nodes[0].getRestApiHttpPort(),
                blockchainRID,
                txRid.toHex())
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

    private fun createBuilder(blockchainRid: BlockchainRid, value: String): GTXDataBuilder {
        val builder = GTXDataBuilder(blockchainRid, arrayOf(KeyPairHelper.pubKey(0)), cryptoSystem)
        builder.addOperation("gtx_test", arrayOf(gtv(1L), gtv(value)))
        builder.finish()
        builder.sign(cryptoSystem.buildSigMaker(KeyPairHelper.pubKey(0), KeyPairHelper.privKey(0)))
        return builder
    }
}