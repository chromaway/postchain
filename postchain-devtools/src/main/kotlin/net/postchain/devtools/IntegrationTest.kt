// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.devtools

import mu.KLogging
import net.postchain.base.PeerInfo
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.common.hexStringToByteArray
import net.postchain.config.app.AppConfig
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.config.node.NodeConfigurationProviderFactory
import net.postchain.core.*
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.gtvml.GtvMLParser
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.privKeyHex
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.devtools.KeyPairHelper.pubKeyHex
import net.postchain.devtools.utils.configuration.UniversalFileLocationStrategy
import org.apache.commons.configuration2.CompositeConfiguration
import org.apache.commons.configuration2.MapConfiguration
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import java.io.File

/**
 * Postchain has different test categories:
 *
 * 1. Unit tests - A test whithout dependencies.
 * 2. Integration tests - Depends on the DB.
 * 3. Nightly test - Tests with a lot of data.
 * 4. Manual test - Requires some manual work to run.
 *
 * Type 2-4 are often heavy, and should inherit this class to get help doing common tasks.
 * Examples of tasks this class will help you with are:
 *
 * - Creating a configuration for:
 *    - single node
 *    - multiple nodes
 * - Verifying all transactions in the BC
 * - Building and committing a block
 * - etc
 */
open class IntegrationTest {

    protected val nodes = mutableListOf<PostchainTestNode>()
    protected val nodesNames = mutableMapOf<String, String>() // { pubKey -> Node${i} }
    val configOverrides = MapConfiguration(mutableMapOf<String, String>())
    val cryptoSystem = SECP256K1CryptoSystem()
    var gtxConfig: Gtv? = null
    protected val blockchainRids = mapOf(
            1L to "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3",
            2L to "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a4"
    )

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
    open fun tearDown() {
        logger.debug("Integration test -- TEARDOWN")
        nodes.forEach { it.shutdown() }
        nodes.clear()
        nodesNames.clear()
        logger.debug("Closed nodes")
        peerInfos = null
        expectedSuccessRids = mutableMapOf()
        configOverrides.clear()
    }

    // TODO: [et]: Check out nullability for return value
    protected fun enqueueTx(node: PostchainTestNode, data: ByteArray, expectedConfirmationHeight: Long): Transaction? {
        val blockchainEngine = node.getBlockchainInstance().getEngine()
        val tx = blockchainEngine.getConfiguration().getTransactionFactory().decodeTransaction(data)
        blockchainEngine.getTransactionQueue().enqueue(tx)

        if (expectedConfirmationHeight >= 0) {
            expectedSuccessRids.getOrPut(expectedConfirmationHeight) { mutableListOf() }
                    .add(tx.getRID())
        }

        return tx
    }

    protected fun verifyBlockchainTransactions(node: PostchainTestNode) {
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

    /**
     * Creates one node from the given configuration.
     * Note: if you want to create many nodes with different configuration, call this method many times
     *
     * @param nodeIndex is a unique number only this node should have (it is used to separate schemas in the DB etc).
     * @param blockchainConfigFilename is the file holding the blockchain's configuration
     * @return the node
     */
    protected fun createNode(nodeIndex: Int, blockchainConfigFilename: String): PostchainTestNode =
            createSingleNode(nodeIndex, 1, DEFAULT_CONFIG_FILE, blockchainConfigFilename)
    /**
     * Creates [count] nodes with the same configuration.
     *
     * @param nodesCount number of nodes to create
     * @param blockchainConfigFilename is the file holding the blockchain's configuration
     * @return an array of nodes
     */
    protected fun createNodes(nodesCount: Int, blockchainConfigFilename: String): Array<PostchainTestNode> =
            Array(nodesCount) { createSingleNode(it, nodesCount, DEFAULT_CONFIG_FILE, blockchainConfigFilename) }

    protected fun createNodesWithReplicas(nodesCount: Int, replicasCount: Int, blockchainConfigFilename: String): Array<PostchainTestNode> {
        val validators = Array(nodesCount) { createSingleNode(it, nodesCount, DEFAULT_CONFIG_FILE, blockchainConfigFilename) }
        val replicas = Array(replicasCount) { createSingleNode(-it - 1, nodesCount, DEFAULT_CONFIG_FILE, blockchainConfigFilename) }
        return validators + replicas
    }

    protected fun createSingleNode(
            nodeIndex: Int,
            totalNodesCount: Int,
            nodeConfig: String,
            blockchainConfigFilename: String,
            preWipeDatabase: Boolean = true
    ): PostchainTestNode {

        val nodeConfigProvider = createNodeConfig(nodeIndex, totalNodesCount, nodeConfig)
        val nodeConfig = nodeConfigProvider.getConfiguration()
        nodesNames[nodeConfig.pubKey] = "$nodeIndex"
        val blockchainConfig = readBlockchainConfig(blockchainConfigFilename)
        val chainId = nodeConfig.activeChainIds.first().toLong()
        val blockchainRid = blockchainRids[chainId]!!.hexStringToByteArray()

        return PostchainTestNode(nodeConfigProvider, preWipeDatabase)
                .apply {
                    addBlockchain(chainId, blockchainRid, blockchainConfig)
                    startBlockchain()
                }
                .also {
                    nodes.add(it)
                }
    }

    protected fun createMultipleChainNodes(
            count: Int,
            nodeConfigsFilenames: Array<String>,
            blockchainConfigsFilenames: Array<String>
    ): Array<PostchainTestNode> {

        require(count == nodeConfigsFilenames.size){ "Must have as many nodes in the array as specified"}

        return Array(count) {
            createMultipleChainNode(it, count, nodeConfigsFilenames[it], *blockchainConfigsFilenames)
        }
    }

    protected fun createMultipleChainNodesWithReplicas(
            nodeCount: Int,
            replicaCount: Int,
            nodeConfigsFilenames: Array<String>,
            blockchainConfigsFilenames: Array<String>
    ): Array<PostchainTestNode> {

        val validators = Array(nodeCount) {
            createMultipleChainNode(it, nodeCount, nodeConfigsFilenames[it], *blockchainConfigsFilenames)
        }

        val replicas = Array(replicaCount) {
            createMultipleChainNode(-it - 1, replicaCount, nodeConfigsFilenames[nodeCount + it], *blockchainConfigsFilenames)
        }

        return validators + replicas
    }

    private fun createMultipleChainNode(
            nodeIndex: Int,
            nodeCount: Int,
            nodeConfigFilename: String = DEFAULT_CONFIG_FILE,
            vararg blockchainConfigFilenames: String,
            preWipeDatabase: Boolean = true
    ): PostchainTestNode {

        val nodeConfigProvider = createNodeConfig(nodeIndex, nodeCount, nodeConfigFilename)
        //require(nodeConfigProvider.getConfiguration().activeChainIds.size == blockchainConfigFilenames.size) {
        //    "The nodes config must have the same number of active chains as the number of specified BC config files."
        //}

        val node = PostchainTestNode(nodeConfigProvider, preWipeDatabase)
                .also { nodes.add(it) }

        nodeConfigProvider.getConfiguration().activeChainIds
                .filter(String::isNotEmpty)
                .forEachIndexed { i, chainId ->
                    val blockchainRid = blockchainRids[chainId.toLong()]!!.hexStringToByteArray()
                    val filename = blockchainConfigFilenames[i]
                    val blockchainConfig = readBlockchainConfig(filename)
                    node.addBlockchain(chainId.toLong(), blockchainRid, blockchainConfig)
                    node.startBlockchain(chainId.toLong())
                }

        return node
    }

    protected fun readBlockchainConfig(blockchainConfigFilename: String): Gtv {
        return GtvMLParser.parseGtvML(
                javaClass.getResource(blockchainConfigFilename).readText())
    }

    protected fun createNodeConfig(nodeIndex: Int, nodeCount: Int = 1, configFile /*= DEFAULT_CONFIG_FILE*/: String)
            : NodeConfigurationProvider {

        // Read first file directly via the builder
        val params = Parameters()
                .fileBased()
//                .setLocationStrategy(ClasspathLocationStrategy())
                .setLocationStrategy(UniversalFileLocationStrategy())
                .setListDelimiterHandler(DefaultListDelimiterHandler(','))
                .setFile(File(configFile))

        val baseConfig = FileBasedConfigurationBuilder(PropertiesConfiguration::class.java)
                .configure(params)
                .configuration

        if (baseConfig.getString("configuration.provider.node") == "legacy") {
            // append nodeIndex to schema name
            val dbSchema = baseConfig.getString("database.schema") + "_" + nodeIndex
            // To convert negative indexes of replica nodes to 'replica_' prefixed indexes.
            baseConfig.setProperty("database.schema", dbSchema.replace("-", "replica_"))

            // peers
            var port = (baseConfig.getProperty("node.0.port") as String).toInt()
            for (i in 0 until nodeCount) {
                baseConfig.setProperty("node.$i.id", "node$i")
                baseConfig.setProperty("node.$i.host", "127.0.0.1")
                baseConfig.setProperty("node.$i.port", port++)
                baseConfig.setProperty("node.$i.pubkey", pubKeyHex(i))
            }
        }

        baseConfig.setProperty("messaging.privkey", privKeyHex(nodeIndex))
        baseConfig.setProperty("messaging.pubkey", pubKeyHex(nodeIndex))

        val appConfig = CompositeConfiguration().apply {
            addConfiguration(configOverrides)
            addConfiguration(baseConfig)
        }

        return NodeConfigurationProviderFactory.createProvider(AppConfig(appConfig))
    }

    protected fun gtxConfigSigners(nodeCount: Int = 1): Gtv {
        return gtv(*Array(nodeCount) { gtv(pubKey(it)) })
    }

    fun createPeerInfosWithReplicas(nodeCount: Int, replicasCount: Int): Array<PeerInfo> {
        if (peerInfos == null) {
            peerInfos =
                    Array(nodeCount) { PeerInfo("localhost", BASE_PORT + it, pubKey(it)) } +
                    Array(replicasCount) { PeerInfo("localhost", BASE_PORT - it - 1, pubKey(-it - 1)) }
        }

        return peerInfos!!
    }

    fun createPeerInfos(nodeCount: Int): Array<PeerInfo> = createPeerInfosWithReplicas(nodeCount, 0)

    protected fun buildBlockAndCommit(engine: BlockchainEngine) {
        val blockBuilder = engine.buildBlock()
        commitBlock(blockBuilder)
    }

    protected fun buildBlockAndCommit(node: PostchainTestNode) {
        commitBlock(node
                .getBlockchainInstance()
                .getEngine()
                .buildBlock())
    }

    private fun commitBlock(blockBuilder: BlockBuilder): BlockWitness {
        val witnessBuilder = blockBuilder.getBlockWitnessBuilder() as MultiSigBlockWitnessBuilder
        val blockData = blockBuilder.getBlockData()
        // Simulate other peers sign the block
        val blockHeader = blockData.header
        var i = 0
        while (!witnessBuilder.isComplete()) {
            val sigMaker =cryptoSystem.buildSigMaker(pubKey(i), privKey(i))
            witnessBuilder.applySignature(sigMaker.signDigest(blockHeader.blockRID))
            i++
        }
        val witness = witnessBuilder.getWitness()
        blockBuilder.commit(witness)
        return witness
    }

    protected fun getTxRidsAtHeight(node: PostchainTestNode, height: Long): Array<ByteArray> {
        val blockQueries = node.getBlockchainInstance().getEngine().getBlockQueries()
        val blockRid = blockQueries.getBlockRids(height).get()
        return blockQueries.getBlockTransactionRids(blockRid!!).get().toTypedArray()
    }

    protected fun getBestHeight(node: PostchainTestNode): Long {
        return node.getBlockchainInstance().getEngine().getBlockQueries().getBestHeight().get()
    }

}