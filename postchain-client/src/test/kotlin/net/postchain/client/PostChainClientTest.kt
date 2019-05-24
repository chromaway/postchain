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
    val defaultSigner = DefaultSigner(cryptoSystem.buildSigMaker(KeyPairHelper.pubKey(0), KeyPairHelper.privKey(0)), KeyPairHelper.pubKey(0))
    val postchainClientFactory =  PostchainClientFactory()

    fun postTransaction(confirmationLevel: ConfirmationLevel) : TransactionResult {
        val nodesCount = 1
        val blocksCount = 1
        val txPerBlock = 1
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        configOverrides.setProperty("api.port", 0)
        createNodes(nodesCount, "/net/postchain/api/blockchain_config_1.xml")
        buildBlockAndCommit(nodes[0])
        val resolver = postchainClientFactory.makeSimpleNodeResolver("http://127.0.0.1:${nodes[0].getRestApiHttpPort()}")

        val b = GTXDataBuilder(blockchainRIDBytes, arrayOf(KeyPairHelper.pubKey(0)), cryptoSystem)
        b.addOperation("gtx_test", arrayOf(GtvFactory.gtv(1L), GtvFactory.gtv("hello${Random().nextLong()}")))
        b.finish()
        b.sign(cryptoSystem.buildSigMaker(KeyPairHelper.pubKey(0), KeyPairHelper.privKey(0)))

        val client = postchainClientFactory.getClient(resolver, blockchainRIDBytes, defaultSigner)
        return client.postTransactionSync(b, confirmationLevel)
    }

    @Test
    fun testPostTransactionApiConfirmLevelNoWait() {
        val result = postTransaction(ConfirmationLevel.NO_WAIT)
        assertEquals(result.status, TransactionStatus.WAITING)
    }

    @Test
    fun testQueryGtxClientApi() {
        val nodesCount = 1
        val blocksCount = 1
        val txPerBlock = 1
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        configOverrides.setProperty("api.port", 0)
        createNodes(nodesCount, "/net/postchain/api/blockchain_config_1.xml")
        buildBlockAndCommit(nodes[0])

        val resolver = postchainClientFactory.makeSimpleNodeResolver("http://127.0.0.1:${nodes[0].getRestApiHttpPort()}")

        val client = postchainClientFactory.getClient(resolver, blockchainRIDBytes, defaultSigner)
        client.query("gtx_test_get_value", gtv("txRID" to gtv("abcd")))

    }
}