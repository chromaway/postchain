// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest

import net.postchain.base.BlockchainRid
import net.postchain.configurations.GTXTestModule
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.KeyPairHelper
import net.postchain.gtv.GtvFactory
import net.postchain.gtx.GTXDataBuilder
import net.postchain.gtx.GTXTransaction
import net.postchain.gtx.GTXTransactionFactory
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Test
import kotlin.test.assertEquals

class BlockchainConfigurationTest : IntegrationTestSetup() {

    @Test
    fun testMaxBlockSize() {
        val blockchainRid = BlockchainRid.buildFromHex("14C483C045F323ACD44298D3BECAAFFD672B1C43D273AB55C0C67F12C9D09014")
        configOverrides.setProperty("infrastructure", "base/test")
        val nodes = createNodes(1, "/net/postchain/devtools/blocks/blockchain_config_max_block_size.xml")
        val node = nodes[0]
        val txQueue = node.getBlockchainInstance().getEngine().getTransactionQueue()

        txQueue.enqueue(buildTransaction(blockchainRid, RandomStringUtils.randomAlphanumeric(1024)))
        buildBlockAndCommit(node)
        assertEquals(0, getBestHeight(node))

        val dummyTest = RandomStringUtils.randomAlphanumeric(25 * 1024 * 1024)

        for (i in 1..2) {
            txQueue.enqueue(buildTransaction(blockchainRid, "${dummyTest}-${i}"))
        }

        try {
            buildBlockAndCommit(node)
        } catch (e: Exception) {
        }

        assertEquals(0, getBestHeight(node))
    }

    @Test
    fun testMaxTransactionSize() {
        val blockchainRid = BlockchainRid.buildFromHex("63E5DE3CBE247D4A57DE19EF751F7840431D680DEC1EC9023B8986E7ECC35412")
        configOverrides.setProperty("infrastructure", "base/test")
        val nodes = createNodes(1, "/net/postchain/devtools/blocks/blockchain_config_max_transaction_size.xml")
        val node = nodes[0]
        val txQueue = node.getBlockchainInstance().getEngine().getTransactionQueue()

        // over 2mb
        txQueue.enqueue(buildTransaction(blockchainRid, "${RandomStringUtils.randomAlphanumeric(1024 * 1024 * 2)}-test"))
        try {
            buildBlockAndCommit(node)
        } catch (e: Exception) {
        }

        assertEquals(-1, getBestHeight(node))

        // less than 2mb
        txQueue.enqueue(buildTransaction(blockchainRid, "${RandomStringUtils.randomAlphanumeric(1024 * 1024)}"))
        buildBlockAndCommit(node)
        assertEquals(0, getBestHeight(node))
    }

    private fun buildTransaction(blockchainRid: BlockchainRid, value: String): GTXTransaction {
        val builder = GTXDataBuilder(blockchainRid, arrayOf(KeyPairHelper.pubKey(0)), cryptoSystem)
        val factory = GTXTransactionFactory(blockchainRid, GTXTestModule(), cryptoSystem)
        builder.addOperation("gtx_test", arrayOf(GtvFactory.gtv(1L), GtvFactory.gtv(value)))
        builder.finish()
        builder.sign(cryptoSystem.buildSigMaker(KeyPairHelper.pubKey(0), KeyPairHelper.privKey(0)))

        return factory.build(builder.getGTXTransactionData())
    }
}