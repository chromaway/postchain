package net.postchain.client

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.core.ProgrammerMistake
import net.postchain.core.TransactionStatus
import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.KeyPairHelper
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GTXDataBuilder
import nl.komponents.kovenant.deferred
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class PostChainClientTest : IntegrationTest() {

    private val blockchainRID = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3"
    private val blockchainRIDBytes = blockchainRID.hexStringToByteArray()
    private val pubKey0 = KeyPairHelper.pubKey(0)
    private val privKey0 = KeyPairHelper.privKey(0)
    private val sigMaker0 = cryptoSystem.buildSigMaker(pubKey0, privKey0)
    private val defaultSigner = DefaultSigner(sigMaker0, pubKey0)
    private val postchainClientFactory = PostchainClientFactory()
    private val randomStr = "hello${Random().nextLong()}"

    private fun createTestNodes(nodesCount: Int, configFileName: String) {
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        configOverrides.setProperty("api.port", 0)
        createNodes(nodesCount, configFileName)
    }

    private fun createGtxDataBuilder(): GTXDataBuilder {
        return GTXDataBuilder(blockchainRIDBytes, arrayOf(pubKey0), cryptoSystem).apply {
            addOperation("gtx_test", arrayOf(gtv(1L), gtv(randomStr)))
            finish()
            sign(sigMaker0)
        }
    }

    private fun createGtxDataBuilder(transaction: String): GTXDataBuilder {
        return GTXDataBuilder(blockchainRIDBytes, arrayOf(pubKey0), cryptoSystem).apply {
            addOperation("gtx_test", arrayOf(gtv(1L), gtv(transaction)))
            finish()
            sign(sigMaker0)
        }
    }

    private fun createPostChainClient(): PostchainClient {
        val resolver = postchainClientFactory.makeSimpleNodeResolver("http://127.0.0.1:${nodes[0].getRestApiHttpPort()}")
        return postchainClientFactory.getClient(resolver, blockchainRIDBytes, defaultSigner)
    }

    @Test
    fun makingAndPostingTransaction_SignedTransactionGiven_PostsSuccessfully() {
        // Mock
        createTestNodes(1, "/net/postchain/devtools/api/blockchain_config_1.xml")
        val client = spy(createPostChainClient())
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
        val client = spy(createPostChainClient())
        val txBuilder = client.makeTransaction()

        // When
        txBuilder.postSync(ConfirmationLevel.NO_WAIT)
    }

    @Test
    fun makingAndPostingSyncTransaction_SignedTransactionGiven_PostsSuccessfully() {
        // Mock
        createTestNodes(1, "/net/postchain/devtools/api/blockchain_config_1.xml")
        val client = spy(createPostChainClient())
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
        createTestNodes(1, "/net/postchain/devtools/api/blockchain_config_1.xml")
        val builder = createGtxDataBuilder()
        val client = createPostChainClient()
        val result = client.postTransactionSync(builder, ConfirmationLevel.NO_WAIT)
        assertEquals(result.status, TransactionStatus.WAITING)
    }

    @Test
    fun testPostTransactionApiConfirmLevelNoWaitPromise() {
        createTestNodes(1, "/net/postchain/devtools/api/blockchain_config_1.xml")
        val builder = createGtxDataBuilder()
        val client = createPostChainClient()
        client.postTransaction(builder, ConfirmationLevel.NO_WAIT).success { resp ->
            assertEquals(resp.status, TransactionStatus.WAITING)
        }
    }

    @Test
    fun testPostTransactionApiConfirmLevelUnverified() {
        createTestNodes(3, "/net/postchain/devtools/api/blockchain_config.xml")
        val builder = createGtxDataBuilder()
        val client = createPostChainClient()
        val result = client.postTransactionSync(builder, ConfirmationLevel.UNVERIFIED)
        assertEquals(result.status, TransactionStatus.CONFIRMED)
    }

    @Test
    fun testPostTransactionApiConfirmLevelUnverifiedPromise() {
        createTestNodes(3, "/net/postchain/devtools/api/blockchain_config.xml")
        val builder = createGtxDataBuilder()
        val client = createPostChainClient()
        client.postTransaction(builder, ConfirmationLevel.UNVERIFIED).success { resp ->
            assertEquals(resp.status, TransactionStatus.CONFIRMED)
        }
    }

    @Test
    fun testQueryGtxClientApiPromise() {
        createTestNodes(3, "/net/postchain/devtools/api/blockchain_config.xml")
        val builder = createGtxDataBuilder()
        val client = createPostChainClient()
        client.postTransactionSync(builder, ConfirmationLevel.UNVERIFIED)
        val gtv = gtv("txRID" to gtv(builder.getDigestForSigning().toHex()))
        client.query("gtx_test_get_value", gtv).success { resp ->
            assertEquals(resp.asString(), randomStr)
        }
    }

    @Test
    fun testMaxTransactionSizeOver() {

        var data3MB = "Lorem Ipsum has been the industry's standard dummy text ever since the 1500s,when an unknown printer took a galley of type and scrambled it to make a type specimen book.It has survived not only five centuries, but also the leap into electronic typesetting,remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages,and more recently with desktop"

        for ( i in 1..13) {
            data3MB += data3MB
        }

        println(data3MB.byteInputStream().readBytes().size)
        createTestNodes(3, "/net/postchain/devtools/api/blockchain_config_max_transaction_size.xml")
        val builder = createGtxDataBuilder(data3MB)
        val client = createPostChainClient()
        val result = client.postTransactionSync(builder, ConfirmationLevel.UNVERIFIED)
        assertEquals(TransactionStatus.REJECTED, result.status)
    }

    @Test
    fun testMaxTransactionSizeOk() {
        createTestNodes(3, "/net/postchain/devtools/api/blockchain_config_max_transaction_size.xml")
        val builder = createGtxDataBuilder()
        val client = createPostChainClient()
        val result = client.postTransactionSync(builder, ConfirmationLevel.UNVERIFIED)
        assertEquals(TransactionStatus.CONFIRMED, result.status)
    }



}