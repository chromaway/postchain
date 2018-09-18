// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.test

import mu.KLogging
import net.postchain.TestNodeEngine
import net.postchain.base.DynamicPortPeerInfo
import net.postchain.base.PeerInfo
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.core.*
import net.postchain.createDataLayer
import net.postchain.gtx.GTXValue
import net.postchain.gtx.gtx
import net.postchain.test.KeyPairHelper.privKey
import net.postchain.test.KeyPairHelper.privKeyHex
import net.postchain.test.KeyPairHelper.pubKey
import net.postchain.test.KeyPairHelper.pubKeyHex
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

    @Deprecated("Legacy")
    protected val nodesLegacy = mutableListOf<TestNodeEngine>()
    protected val nodes = mutableListOf<PostchainTestNode>()
    val configOverrides = MapConfiguration(mutableMapOf<String, String>())
    val cryptoSystem = SECP256K1CryptoSystem()
    var gtxConfig: GTXValue? = null

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
    protected fun enqueueTx(node: PostchainTestNode, chainId: Long, data: ByteArray, expectedConfirmationHeight: Long): Transaction? {
        val blockchain = node.getBlockchainInstance(chainId)
        val tx = blockchain.blockchainConfiguration.getTransactionFactory().decodeTransaction(data)
        blockchain.getEngine().getTransactionQueue().enqueue(tx)

        if (expectedConfirmationHeight >= 0) {
            expectedSuccessRids.getOrPut(expectedConfirmationHeight) { mutableListOf() }
                    .add(tx.getRID())
        }

        return tx
    }

    protected fun verifyBlockchainTransactions(node: PostchainTestNode, chainId: Long) {
        val expectAtLeastHeight = expectedSuccessRids.keys.reduce { acc, l -> maxOf(l, acc) }
        val bestHeight = getBestHeight(node, chainId)
        assertTrue(bestHeight >= expectAtLeastHeight)
        for (height in 0..bestHeight) {
            val txRidsAtHeight = getTxRidsAtHeight(node, chainId, height)

            val expectedRidsAtHeight = expectedSuccessRids.get(height)
            if (expectedRidsAtHeight == null) {
                assertArrayEquals(arrayOf(), txRidsAtHeight)
            } else {
                assertArrayEquals(expectedRidsAtHeight.toTypedArray(), txRidsAtHeight)
            }
        }
    }

    protected fun createNode(nodeIndex: Int): Pair<PostchainTestNode, Long> =
            createSingleNode(nodeIndex, 1)

    protected fun createNodes(count: Int): Array<Pair<PostchainTestNode, Long>> =
            Array(count) { createSingleNode(it, count) }

    private fun createSingleNode(nodeIndex: Int, totalNodesCount: Int): Pair<PostchainTestNode, Long> {
        val config = createConfig(nodeIndex, totalNodesCount, DEFAULT_CONFIG_FILE)
        val chainId = chainId(config)
        return PostchainTestNode(config).apply {
            startBlockchain(chainId)
            nodes.add(this)
        } to chainId
    }

    @Deprecated("Legacy code")
    protected fun createEngines(count: Int): Array<PostchainTestNode> {
        return Array(count) {
            val config = createConfig(it, configFile = DEFAULT_CONFIG_FILE)
            PostchainTestNode(config).apply {
                startBlockchain(chainId(config))
                nodes.add(this)
            }
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
        baseConfig.setProperty("blockchain.$chainId.signers", Array(nodeCount, { pubKeyHex(it) }).reduce({ acc, value -> "$acc,$value" }))
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

    // TODO: [et]: Remove legacy
    @Deprecated("Legacy")
    protected fun createDataLayer(nodeIndex: Int, nodeCount: Int = 1, configFile: String = DEFAULT_CONFIG_FILE): TestNodeEngine {
        val config = createConfig(nodeIndex, nodeCount, configFile)
        val chainId = config.getLong("activechainids")

        val dataLayer = createDataLayer(config, chainId, nodeIndex)

        // keep list of nodes to shutdown after test
        nodesLegacy.add(dataLayer)
        return dataLayer
    }

    protected fun gtxConfigSigners(nodeCount: Int = 1): GTXValue {
        return gtx(*Array(nodeCount) { gtx(pubKey(it)) })
    }

    /*
    @Deprecated("Legacy")
    protected fun createDataLayerNG(nodeIndex: Int, nodeCount: Int = 1, configFile: String = DEFAULT_CONFIG_FILE): TestNodeEngine {
        val config = createConfig(nodeIndex, nodeCount, configFile)
        val chainId = config.getLong("activechainids")

        val infrastructure = BaseBlockchainInfrastructureFactory().makeBlockchainInfrastructure(config)

        val blockchainRID = config.getString("blockchain.1.blockchainrid").hexStringToByteArray() // TODO

        val dataLayer = createTestNodeEngine(infrastructure as BaseBlockchainInfrastructure,
                gtxConfig!!, BaseBlockchainContext(blockchainRID, nodeIndex, chainId, null) // TODO
        )

        // keep list of nodes to shutdown after test
        nodesLegacy.add(dataLayer)
        return dataLayer
    }
    */

    /*
        protected fun createBasePeerCommConfiguration(nodeCount: Int, myIndex: Int): BasePeerCommConfiguration {
            val peerInfos = createPeerInfos(nodeCount)
            val privKey = privKey(myIndex)
            return BasePeerCommConfiguration(peerInfos, myIndex, SECP256K1CryptoSystem(), privKey)
        }
    */
    fun createPeerInfos(nodeCount: Int): Array<PeerInfo> {
        if (peerInfos == null) {
            val pubKeysToUse = Array<ByteArray>(nodeCount, { pubKey(it) })
            peerInfos = Array<PeerInfo>(nodeCount, { DynamicPortPeerInfo("localhost", pubKeysToUse[it]) })
        }
        return peerInfos!!
    }

    /*
        protected fun arrayOfBasePeerCommConfigurations(count: Int): Array<BasePeerCommConfiguration> {
            return Array(count, { createBasePeerCommConfiguration(count, it) })
        }
    */
    protected fun buildBlockAndCommit(node: TestNodeEngine) {
        buildBlockAndCommit(node.engine)
    }

    protected fun buildBlockAndCommit(engine: BlockchainEngine) {
        val blockBuilder = engine.buildBlock()
        commitBlock(blockBuilder)
    }

    protected fun buildBlockAndCommit(node: PostchainTestNode, chainId: Long) {
        commitBlock(node
                .getBlockchainInstance(chainId)
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

    protected fun getTxRidsAtHeight(node: PostchainTestNode, chainId: Long, height: Long): Array<ByteArray> {
        val blockQueries = node.getBlockchainInstance(chainId).getEngine().getBlockQueries()
        val list = blockQueries.getBlockRids(height).get()
        return blockQueries.getBlockTransactionRids(list[0]).get().toTypedArray()
    }

    protected fun getBestHeight(node: PostchainTestNode, chainId: Long): Long {
        return node.getBlockchainInstance(chainId).getEngine().getBlockQueries().getBestHeight().get()
    }

    protected fun chainId(config: Configuration): Long {
        return config.getLong("activechainids")
    }
}