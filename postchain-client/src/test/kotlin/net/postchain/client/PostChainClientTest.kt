package net.postchain.client

import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.core.TransactionStatus
import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.KeyPairHelper
import net.postchain.gtv.GtvFactory
import net.postchain.gtx.EMPTY_SIGNATURE
import net.postchain.gtx.GTXDataBuilder
import net.postchain.integrationtest.JsonTools
import net.postchain.gtv.GtvFactory.gtv
import org.junit.Test
import org.spongycastle.crypto.tls.ConnectionEnd.client
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PostChainClientTest : IntegrationTest() {

    private val gson = JsonTools.buildGson()
    private val blockchainRID = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3"
    private val blockchainRIDBytes = blockchainRID.hexStringToByteArray()
    private val privateKey = "03a301697bdfcd704313ba48e51d567543f2a182031efd6915ddc07bbcc4e16070"
    private val defaultSigner = DefaultSigner(cryptoSystem.buildSigMaker(KeyPairHelper.pubKey(0), KeyPairHelper.privKey(0)), KeyPairHelper.pubKey(0))
    private val postchainClientFactory =  PostchainClientFactory()

    private fun createNodesTest(nodesCount: Int, configFileName: String) {
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        configOverrides.setProperty("api.port", 0)
        createNodes(nodesCount, configFileName)
    }

    private fun createGtxDataBuiler() :GTXDataBuilder {
        val b = GTXDataBuilder(blockchainRIDBytes, arrayOf(KeyPairHelper.pubKey(0)), cryptoSystem)
        b.addOperation("gtx_test", arrayOf(GtvFactory.gtv(1L), GtvFactory.gtv("hello${Random().nextLong()}")))
        b.finish()
        b.sign(cryptoSystem.buildSigMaker(KeyPairHelper.pubKey(0), KeyPairHelper.privKey(0)))
        return b
    }

    private fun createPostChainClientTest() : PostchainClient {
        val resolver = postchainClientFactory.makeSimpleNodeResolver("http://127.0.0.1:${nodes[0].getRestApiHttpPort()}")
        return postchainClientFactory.getClient(resolver, blockchainRIDBytes, defaultSigner)
    }

    @Test
    fun testPostTransactionApiConfirmLevelNoWait() {
        createNodesTest(1, "/net/postchain/api/blockchain_config_1.xml")
        val b = createGtxDataBuiler()
        val client = createPostChainClientTest()
        val result = client.postTransactionSync(b, ConfirmationLevel.NO_WAIT)
        assertEquals(result.status, TransactionStatus.WAITING)
    }

    @Test
    fun testPostTransactionApiConfirmLevelNoWaitPromise() {
        createNodesTest(1, "/net/postchain/api/blockchain_config_1.xml")
        val b = createGtxDataBuiler()
        val client = createPostChainClientTest()
        client.postTransaction(b, ConfirmationLevel.NO_WAIT).success {
            it -> assertEquals(it.status, TransactionStatus.WAITING)
        }
    }

    @Test
    fun testPostTransactionApiConfirmLevelUnverifiedPromise() {
        createNodesTest(3, "/net/postchain/api/blockchain_config.xml")
        val b = createGtxDataBuiler()
        val client = createPostChainClientTest()
        client.postTransaction(b, ConfirmationLevel.UNVERIFIED).success {
            it -> assertEquals(it.status, TransactionStatus.CONFIRMED)
        }
    }

    @Test
    fun testPostTransactionApiConfirmLevelUnverified() {
        createNodesTest(3, "/net/postchain/api/blockchain_config.xml")
        val b = createGtxDataBuiler()
        val client = createPostChainClientTest()
        val result = client.postTransactionSync(b, ConfirmationLevel.UNVERIFIED)
        assertEquals(result.status, TransactionStatus.CONFIRMED)
    }

    @Test
    fun testQueryGtxClientApi() {
        createNodesTest(1, "/net/postchain/api/blockchain_config_1.xml")
        val b = createGtxDataBuiler()
        val client = createPostChainClientTest()
        client.postTransactionSync(b, ConfirmationLevel.NO_WAIT)
        val gtv = client.query("gtx_test_get_value", gtv("txRID" to gtv(b.getDigestForSigning().toHex())))
    }
}