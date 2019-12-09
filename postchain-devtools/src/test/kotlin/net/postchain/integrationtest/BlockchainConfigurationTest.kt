package net.postchain.integrationtest

import net.postchain.base.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.configurations.GTXTestModule
import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.KeyPairHelper
import net.postchain.gtv.GtvFactory
import net.postchain.gtx.GTXDataBuilder
import net.postchain.gtx.GTXTransaction
import net.postchain.gtx.GTXTransactionFactory
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Test
import kotlin.test.assertEquals

class BlockchainConfigurationTest : IntegrationTest() {

    private val blockchainRID = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3"
    private val blockchainRIDBytes = blockchainRID.hexStringToByteArray()

    @Test
    fun testMaxBlockSize() {
        configOverrides.setProperty("infrastructure", "base/test")
        val node = createNode(0, "/net/postchain/devtools/blocks/blockchain_config_max_block_size.xml")
        val txQueue = node.getBlockchainInstance().getEngine().getTransactionQueue()

        txQueue.enqueue(buildTransaction(RandomStringUtils.randomAlphanumeric(1024)))
        buildBlockAndCommit(node)
        assertEquals(0, getBestHeight(node))

        val dummyTest = RandomStringUtils.randomAlphanumeric(1024 * 1024)

        for (i in 1..2) {
            txQueue.enqueue(buildTransaction("${dummyTest}-${i}"))
        }

        try {
            buildBlockAndCommit(node)
        } catch (e: Exception) {
        }

        assertEquals(0, getBestHeight(node))
    }

    @Test
    fun testMaxTransactionSize() {
        configOverrides.setProperty("infrastructure", "base/test")
        val node = createNode(0, "/net/postchain/devtools/blocks/blockchain_config_max_transaction_size.xml")
        val txQueue = node.getBlockchainInstance().getEngine().getTransactionQueue()

        // over 2mb
        txQueue.enqueue(buildTransaction("${RandomStringUtils.randomAlphanumeric(1024 * 1024 * 2)}-test"))
        try {
            buildBlockAndCommit(node)
        } catch (e: Exception) {
        }

        assertEquals(-1, getBestHeight(node))

        // less than 2mb
        txQueue.enqueue(buildTransaction("${RandomStringUtils.randomAlphanumeric(1024 * 1024)}"))
        buildBlockAndCommit(node)
        assertEquals(0, getBestHeight(node))
    }

    private fun buildTransaction(value: String): GTXTransaction {
        val builder = GTXDataBuilder(BlockchainRid.buildFromHex(blockchainRID), arrayOf(KeyPairHelper.pubKey(0)), cryptoSystem)
        val factory = GTXTransactionFactory(BlockchainRid.buildFromHex(blockchainRID), GTXTestModule(), cryptoSystem)
        builder.addOperation("gtx_test", arrayOf(GtvFactory.gtv(1L), GtvFactory.gtv(value)))
        builder.finish()
        builder.sign(cryptoSystem.buildSigMaker(KeyPairHelper.pubKey(0), KeyPairHelper.privKey(0)))

        return factory.build(builder.getGTXTransactionData())
    }
}