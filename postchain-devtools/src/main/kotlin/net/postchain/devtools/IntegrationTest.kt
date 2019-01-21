// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.devtools

import mu.KLogging
import net.postchain.base.PeerInfo
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.core.*
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.privKeyHex
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.devtools.KeyPairHelper.pubKeyHex
import net.postchain.gtx.GTXValue
import net.postchain.gtx.gtx
import net.postchain.gtx.gtxml.GTXMLValueParser
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
    var gtxConfig: GTXValue? = null

    // PeerInfos must be shared between all nodes because
    // a listening node will update the PeerInfo port after
    // ServerSocket is created.
    private var peerInfos: Array<PeerInfo>? = null
    private var expectedSuccessRids = mutableMapOf<Long, MutableList<ByteArray>>()

    companion object : KLogging() {
        const val BASE_PORT = 9870
        const val DEFAULT_CONFIG_FILE = "config.properties"
        const val DEFAULT_BLOCKCHAIN_CONFIG_FILE = "blockchain_config.xml"
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

    protected fun createNode(nodeIndex: Int, blockchainConfigFilename: String): SingleChainTestNode =
            createSingleNode(nodeIndex, 1, blockchainConfigFilename)

    protected fun createNodes(count: Int, blockchainConfigFilename: String): Array<SingleChainTestNode> =
            Array(count) { createSingleNode(it, count, blockchainConfigFilename) }

    private fun createSingleNode(nodeIndex: Int, totalNodesCount: Int, blockchainConfigFilename: String): SingleChainTestNode {
        val nodeConfig = createConfig(
                nodeIndex, totalNodesCount, DEFAULT_CONFIG_FILE)

        val blockchainConfig = GTXMLValueParser.parseGTXMLValue(
                javaClass.getResource(blockchainConfigFilename).readText())

        return SingleChainTestNode(nodeConfig, blockchainConfig)
                .apply { startBlockchain() }
                .also { nodes.add(it) }
    }

    protected fun createConfig(nodeIndex: Int, nodeCount: Int = 1, configFile /*= DEFAULT_CONFIG_FILE*/: String)
            : Configuration {

        val propertiesFile = File(configFile)
        val params = Parameters()
                .fileBased()
                .setLocationStrategy(ClasspathLocationStrategy())
                .setFile(propertiesFile)
        // Read first file directly via the builder
        val baseConfig = FileBasedConfigurationBuilder(PropertiesConfiguration::class.java)
                .configure(params)
                .configuration

        baseConfig.listDelimiterHandler = DefaultListDelimiterHandler(',')
        val chainId = baseConfig.getLong("activechainids")
        val signers = Array(nodeCount) { pubKeyHex(it) }.joinToString(",")
//        baseConfig.setProperty("blockchain.$chainId.signers", signers)
        // append nodeIndex to schema name
        baseConfig.setProperty("database.schema", baseConfig.getString("database.schema") + nodeIndex)
//        baseConfig.setProperty("blocksigningprivkey", privKeyHex(nodeIndex)) // TODO: newschool
//        baseConfig.setProperty("blockchain.$chainId.blocksigningprivkey", privKeyHex(nodeIndex)) // TODO: oldschool

        // peers
        var port = (baseConfig.getProperty("node.0.port") as String).toInt()
        for (i in 0 until nodeCount) {
            baseConfig.setProperty("node.$i.id", "node$i")
            baseConfig.setProperty("node.$i.host", "127.0.0.1")
            baseConfig.setProperty("node.$i.port", port++)
            baseConfig.setProperty("node.$i.pubkey", pubKeyHex(i))
        }
//        baseConfig.setProperty("blockchain.$chainId.testmyindex", nodeIndex)

        configOverrides.setProperty("messaging.privkey", privKeyHex(nodeIndex))

        return CompositeConfiguration().apply {
            addConfiguration(configOverrides)
            addConfiguration(baseConfig)

        }
    }

    protected fun gtxConfigSigners(nodeCount: Int = 1): GTXValue {
        return gtx(*Array(nodeCount) { gtx(pubKey(it)) })
    }

    fun createPeerInfos(nodeCount: Int): Array<PeerInfo> {
        if (peerInfos == null) {
            peerInfos = Array(nodeCount) {
                // TODO: Fix this hack
                PeerInfo("localhost", BASE_PORT + it, pubKey(it))
            }
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