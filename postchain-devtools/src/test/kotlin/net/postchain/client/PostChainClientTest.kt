// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.client

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import net.postchain.base.BlockchainRid
import net.postchain.client.core.ConfirmationLevel
import net.postchain.client.core.DefaultSigner
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientFactory
import net.postchain.common.toHex
import net.postchain.core.ProgrammerMistake
import net.postchain.core.TransactionStatus
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.KeyPairHelper
import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.utils.configuration.system.SystemSetupFactory
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GTXDataBuilder
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class PostChainClientTest : IntegrationTestSetup() {

    private val blockchainRIDStr = "ABABABABABABABABABABABABABABABABABABABABABABABABABABABABABABABAB"
    private val blockchainRID = BlockchainRid.buildFromHex(blockchainRIDStr)
    private val pubKey0 = KeyPairHelper.pubKey(0)
    private val privKey0 = KeyPairHelper.privKey(0)
    private val sigMaker0 = cryptoSystem.buildSigMaker(pubKey0, privKey0)
    private val defaultSigner = DefaultSigner(sigMaker0, pubKey0)
    private val randomStr = "hello${Random().nextLong()}"

    private fun createTestNodes(nodesCount: Int, configFileName: String): Array<PostchainTestNode> {
        val sysSetup =  SystemSetupFactory.buildSystemSetup(mapOf(1 to configFileName))
        sysSetup.needRestApi = true
        assertEquals(nodesCount, sysSetup.nodeMap.size)
        createNodesFromSystemSetup(sysSetup)
        return nodes.toTypedArray()
    }

    private fun createGtxDataBuilder(bcRid: BlockchainRid): GTXDataBuilder {
        return GTXDataBuilder(bcRid, arrayOf(pubKey0), cryptoSystem).apply {
            addOperation("gtx_test", arrayOf(gtv(1L), gtv(randomStr)))
            finish()
            sign(sigMaker0)
        }
    }

    private fun createPostChainClient(bcRid: BlockchainRid): PostchainClient {
        val resolver = PostchainClientFactory.makeSimpleNodeResolver("http://127.0.0.1:${nodes[0].getRestApiHttpPort()}")
        return PostchainClientFactory.getClient(resolver, bcRid, defaultSigner)
    }

    @Test
    fun makingAndPostingTransaction_SignedTransactionGiven_PostsSuccessfully() {
        // Mock
        val nodes = createTestNodes(1, "/net/postchain/devtools/api/blockchain_config_1.xml")
        val client = spy(createPostChainClient(blockchainRID))
        val txBuilder = client.makeTransaction()

        // When
        txBuilder.post(ConfirmationLevel.NO_WAIT).success {
            // Then
            verify(client).postTransaction(any(), eq(ConfirmationLevel.NO_WAIT))
        }
    }

    @Test(expected = ProgrammerMistake::class)
    fun makingAndPostingSyncTransaction_UnsignedTransactionGiven_throws_Exception() {
        // Mock
        createTestNodes(1, "/net/postchain/devtools/api/blockchain_config_1.xml")
        val client = spy(createPostChainClient(blockchainRID))
        val txBuilder = client.makeTransaction()

        // When
        txBuilder.postSync(ConfirmationLevel.NO_WAIT)
    }

    @Test
    fun makingAndPostingSyncTransaction_SignedTransactionGiven_PostsSuccessfully() {
        // Mock
        createTestNodes(1, "/net/postchain/devtools/api/blockchain_config_1.xml")
        val client = spy(createPostChainClient(blockchainRID))
        val txBuilder = client.makeTransaction()

        txBuilder.addOperation("nop", arrayOf())
        txBuilder.addOperation("nop", arrayOf())
        txBuilder.sign(sigMaker0)

        // When
        txBuilder.postSync(ConfirmationLevel.NO_WAIT)

        // Then
        verify(client).postTransactionSync(any(), eq(ConfirmationLevel.NO_WAIT))
    }

    @Test
    fun testPostTransactionApiConfirmLevelNoWait() {
        val nodes = createTestNodes(1, "/net/postchain/devtools/api/blockchain_config_1.xml")
        val blockchainRid = systemSetup.blockchainMap[1]!!.rid
        val builder = createGtxDataBuilder(blockchainRid!!)
        val client = createPostChainClient(blockchainRid)
        val result = client.postTransactionSync(builder, ConfirmationLevel.NO_WAIT)
        assertEquals(TransactionStatus.WAITING, result.status)
    }

    @Test
    fun testPostTransactionApiConfirmLevelNoWaitPromise() {
        val nodes = createTestNodes(1, "/net/postchain/devtools/api/blockchain_config_1.xml")
        val blockchainRid = systemSetup.blockchainMap[1]!!.rid
        val builder = createGtxDataBuilder(blockchainRid)
        val client = createPostChainClient(blockchainRid)

        await().untilCallTo {
            client.postTransaction(builder, ConfirmationLevel.NO_WAIT).get()
        } matches { resp ->
            resp?.status == TransactionStatus.WAITING
        }
    }

    @Test
    fun testPostTransactionApiConfirmLevelUnverified() {
        val nodes = createTestNodes(3, "/net/postchain/devtools/api/blockchain_config.xml")
        val blockchainRid = systemSetup.blockchainMap[1]!!.rid
        val builder = createGtxDataBuilder(blockchainRid)
        val client = createPostChainClient(blockchainRid)
        val result = client.postTransactionSync(builder, ConfirmationLevel.UNVERIFIED)
        assertEquals(TransactionStatus.CONFIRMED, result.status)
    }

    @Test
    fun testPostTransactionApiConfirmLevelUnverifiedPromise() {
        val nodes = createTestNodes(3, "/net/postchain/devtools/api/blockchain_config.xml")
        val blockchainRid = systemSetup.blockchainMap[1]!!.rid
        val builder = createGtxDataBuilder(blockchainRid)
        val client = createPostChainClient(blockchainRid)

        await().untilCallTo {
            client.postTransaction(builder, ConfirmationLevel.UNVERIFIED).get()
        } matches { resp ->
            resp?.status == TransactionStatus.CONFIRMED
        }
    }

    @Test
    fun testQueryGtxClientApiPromise() {
        val nodes = createTestNodes(3, "/net/postchain/devtools/api/blockchain_config.xml")
        val blockchainRid = systemSetup.blockchainMap[1]!!.rid
        val builder = createGtxDataBuilder(blockchainRid)
        val client = createPostChainClient(blockchainRid)
        client.postTransactionSync(builder, ConfirmationLevel.UNVERIFIED)
        val gtv = gtv("txRID" to gtv(builder.getDigestForSigning().toHex()))

        await().untilCallTo {
            client.query("gtx_test_get_value", gtv).get()
        } matches { resp ->
            resp?.asString() == randomStr
        }
    }
}