// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.devtools

import mu.KLogging
import net.postchain.base.DynamicPortPeerInfo
import net.postchain.base.PeerInfo
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.core.*
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.privKeyHex
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.devtools.KeyPairHelper.pubKeyHex
import org.apache.commons.configuration2.CompositeConfiguration
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.MapConfiguration
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler
import org.apache.commons.configuration2.io.ClasspathLocationStrategy
import org.junit.After
import org.junit.Assert.*
import java.io.File


open class IntegrationTest {

    protected val nodes = mutableListOf<SingleChainTestNode>()
    val configOverrides = MapConfiguration(mutableMapOf<String, String>())
    val cryptoSystem = SECP256K1CryptoSystem()
    var gtxConfig: Gtv? = null

    // PeerInfos must be shared between all nodes because
    // a listening node will update the PeerInfo port after
    // ServerSocket is created.
    private var peerInfos: Array<PeerInfo>? = null
    private var expectedSuccessRids = mutableMapOf<Long, MutableList<ByteArray>>()

    companion object : KLogging() {
        const val DEFAULT_CONFIG_FILE = "config.properties"
    }

    @After
    fun tearDown() {
        nodes.forEach { it.stopAllBlockchain() }
        nodes.clear()
        logger.debug("Closed nodes")
        peerInfos = null
        expectedSuccessRids = mutableMapOf()
        configOverrides.clear()
    }

    // TODO: [et]: Check out nullability for return value
    protected fun enqueueTx(node: SingleChainTestNode, data: ByteArray, expectedConfirmationHeight: Long): Transaction? {
        val blockchain = node.getBlockchainInstance()
        val tx = blockchain.blockchainConfiguration.getTransactionFactory().decodeTransaction(data)
        blockchain.getEngine().getTransactionQueue().enqueue(tx)

        if (expectedConfirmationHeight >= 0) {
            expectedSuccessRids.getOrPut(expectedConfirmationHeight) { mutableListOf() }
                    .add(tx.getRID())
        }

        return tx
    }

    protected fun verifyBlockchainTransactions(node: SingleChainTestNode) {
        val expectAtLeastHeight = expectedSuccessRids.keys.reduce { acc, l -> maxOf(l, acc) }
        val bestHeight = getBestHeight(node)
        assertTrue(bestHeight >= expectAtLeastHeight)
        for (height in 0..bestHeight) {
            val txRidsAtHeight = getTxRidsAtHeight(node, height)

            val expectedRidsAtHeight = expectedSuccessRids[height]
            if (expectedRidsAtHeight == null) {
                assertArrayEquals(arrayOf(), txRidsAtHeight)
            } else {
                assertArrayEquals(expectedRidsAtHeight.toTypedArray(), txRidsAtHeight)
            }
        }
    }

    protected fun createNode(nodeIndex: Int): SingleChainTestNode =
            createSingleNode(nodeIndex, 1)

    protected fun createNodes(count: Int): Array<SingleChainTestNode> =
            Array(count) { createSingleNode(it, count) }

    private fun createSingleNode(nodeIndex: Int, totalNodesCount: Int): SingleChainTestNode {
        val config = createConfig(nodeIndex, totalNodesCount, DEFAULT_CONFIG_FILE)
        return SingleChainTestNode(config).apply {
            startBlockchain()
            nodes.add(this)
        }
    }

    protected fun createConfig(nodeIndex: Int, nodeCount: Int = 1, configFile /*= DEFAULT_CONFIG_FILE*/: String)
            : Configuration {
        val propertiesFile = File(configFile)
        val params = Parameters()
        // Read first file directly via the builder
        val builder = FileBasedConfigurationBuilder(PropertiesConfiguration::class.java)
                .configure(params
                        .fileBased()
                        .setLocationStrategy(ClasspathLocationStrategy())
                        .setFile(propertiesFile))
        val baseConfig = builder.configuration

        baseConfig.listDelimiterHandler = DefaultListDelimiterHandler(',')
        val chainId = baseConfig.getLong("activechainids")
        val signers = Array(nodeCount) { pubKeyHex(it) }.joinToString(",")
        baseConfig.setProperty("blockchain.$chainId.signers", signers)
        // append nodeIndex to schema name
        baseConfig.setProperty("database.schema", baseConfig.getString("database.schema") + nodeIndex)
        baseConfig.setProperty("blocksigningprivkey", privKeyHex(nodeIndex)) // TODO: newschool
        baseConfig.setProperty("blockchain.$chainId.blocksigningprivkey", privKeyHex(nodeIndex)) // TODO: oldschool
        for (i in 0 until nodeCount) {
            baseConfig.setProperty("node.$i.id", "node$i")
            baseConfig.setProperty("node.$i.host", "127.0.0.1")
            baseConfig.setProperty("node.$i.port", "0")
            baseConfig.setProperty("node.$i.pubkey", pubKeyHex(i))
        }
        baseConfig.setProperty("blockchain.$chainId.testmyindex", nodeIndex)

        configOverrides.setProperty("messaging.privkey", privKeyHex(nodeIndex))

        return CompositeConfiguration().apply {
            addConfiguration(configOverrides)
            addConfiguration(baseConfig)
        }
    }

    protected fun gtxConfigSigners(nodeCount: Int = 1): Gtv {
        return gtv(*Array(nodeCount) { gtv(pubKey(it)) })
    }

    fun createPeerInfos(nodeCount: Int): Array<PeerInfo> {
        if (peerInfos == null) {
            peerInfos = Array(nodeCount) { DynamicPortPeerInfo("localhost", pubKey(it)) }
        }

        return peerInfos!!
    }

    protected fun buildBlockAndCommit(engine: BlockchainEngine) {
        val blockBuilder = engine.buildBlock()
        commitBlock(blockBuilder)
    }

    protected fun buildBlockAndCommit(node: SingleChainTestNode) {
        commitBlock(node
                .getBlockchainInstance()
                .getEngine()
                .buildBlock())
    }

    private fun commitBlock(blockBuilder: BlockBuilder): BlockWitness {
        val witnessBuilder = blockBuilder.getBlockWitnessBuilder() as MultiSigBlockWitnessBuilder
        assertNotNull(witnessBuilder)
        val blockData = blockBuilder.getBlockData()
        // Simulate other peers sign the block
        val blockHeader = blockData.header
        var i = 0
        while (!witnessBuilder.isComplete()) {
            witnessBuilder.applySignature(cryptoSystem.makeSigner(pubKey(i), privKey(i))(blockHeader.rawData))
            i++
        }
        val witness = witnessBuilder.getWitness()
        blockBuilder.commit(witness)
        return witness
    }

    protected fun getTxRidsAtHeight(node: SingleChainTestNode, height: Long): Array<ByteArray> {
        val blockQueries = node.getBlockchainInstance().getEngine().getBlockQueries()
        val list = blockQueries.getBlockRids(height).get()
        return blockQueries.getBlockTransactionRids(list[0]).get().toTypedArray()
    }

    protected fun getBestHeight(node: SingleChainTestNode): Long {
        return node.getBlockchainInstance().getEngine().getBlockQueries().getBestHeight().get()
    }

}