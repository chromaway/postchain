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
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.devtools.utils.configuration.*
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.gtvml.GtvMLParser
import org.apache.commons.configuration2.CompositeConfiguration
import org.apache.commons.configuration2.MapConfiguration
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue

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
    protected val nodeMap = mutableMapOf<NodeSeqNumber, PostchainTestNode>()
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
        System.gc()
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

    /**
     * Starts [count] nodes with the same number of chains for each node
     *
     * @param count is the number of nodes
     * @param nodeConfigsFilenames an array with one config file path per node
     * @param blockchainConfigsFilenames an array with one config file path per blockchain
     */
    protected fun createMultipleChainNodes(
            count: Int,
            nodeConfigsFilenames: Array<String>,
            blockchainConfigsFilenames: Array<String>
    ): Array<PostchainTestNode> {

        require(count == nodeConfigsFilenames.size) { "Must have as many nodes in the array as specified" }

        return Array(count) {
            createMultipleChainNode(it, count, nodeConfigsFilenames[it], *blockchainConfigsFilenames)
        }
    }

    /**
     * Starts the nodes with the number of chains different for each node
     *
     * NOTE: This is a more generic function compared to the [createMultipleChainNodes] function. It handles any setup.
     *
     * @param systemSetup is holds the configuration of all the nodes and chains
     */
    protected fun createMultipleChainNodesFromSystemSetup(systemSetup: SystemSetup, nodeConfProv: NodeConfigurationProvider) = systemSetup.toTestNodes(nodeConfProv).toTypedArray()


    /**
     * Can be called directly from the test.
     *
     * @return Array of [PostchainTestNode] matching the node count and replica count and the blockchain config.
     */
    protected fun createMultipleChainNodesWithReplicas(
            nodeCount: Int,
            replicaCount: Int,
            blockchainConfigsFilenames: Array<String>
    ): Array<PostchainTestNode> {

        val blockchainSetups  = mutableListOf<BlockchainSetup>()
        val chainIds = listOf<Int>()
        for (bcFile in blockchainConfigsFilenames) {
            val bcGtv = readBlockchainConfig(bcFile)
            val bcSetup = BlockchainSetup.buildFromGtv(bcGtv)
            blockchainSetups.add(bcSetup)
        }

        // 1.a Build the setup without replicas
        val systemSetupWithOnlySigners = SystemSetup.buildSimpleSetup(nodeCount, chainIds)

        // 1.b Add the replicas to setup
        val systemSetup = if (replicaCount > 0) {
            val replicaMap = mutableMapOf<NodeSeqNumber, NodeSetup>()
            for (replicaNr in 1..replicaCount) {
                val replSeqNr = NodeSeqNumber(replicaNr * -1) // We use negative numbers for replicas, just to make them easy to identify
                val replica = NodeSetup.buildSimple(replSeqNr, setOf(), chainIds.toSet()) // Make the replica signer of nothing, but replicate everything.
                replicaMap[replSeqNr] = replica
            }

            SystemSetup.addNodesToSystemSetup(replicaMap.toMap(), systemSetupWithOnlySigners)
        } else {
            systemSetupWithOnlySigners
        }

        // 2 Create the configuraton provider
        val configOverrides: MapConfiguration()
        addConfigProviderToNodeSetups(systemSetup, configOverrides)

        // 3. Create all PostchainTestNodes from setup
        return systemSetup.toTestNodes().toTypedArray()
    }

    /**
     * Takes a [SystemSetup] and adds [NodeConfigurationProvider] to all [NodeSetup] in it.
     */
    protected fun addConfigProviderToNodeSetups(
            systemSetup: SystemSetup,
            configOverrides: MapConfiguration
    )  {
        val testName = this::class.simpleName!!
        for (nodeSetup in systemSetup.nodeMap.values) {

            val nodeConfProv: NodeConfigurationProvider = NodeConfigurationProviderGenerator.buildFromSetup(
                    testName,
                    configOverrides,
                    nodeSetup,
                    systemSetup)
            nodeSetup.configurationProvider = nodeConfProv // TODO: A bit ugly to mutate an existing instance like this. Ideas?
        }

    }

    /**
     * @param nodeIndex is this node's index number
     * @param nodesThisNodeKnowsAbout is a list of other nodes' ID that this node should connect to
     * @param nodeConfigFilename
     * @param blockchainConfigFilenames
     * @param preWipeDatabase
     */
    private fun createMultipleChainNode(
            nodeIndex: Int,
            nodesThisNodeKnowsAbout: List<Int>,
            nodeConfigFilename: String = DEFAULT_CONFIG_FILE,
            vararg blockchainConfigFilenames: String,
            preWipeDatabase: Boolean = true
    ): PostchainTestNode {

        val nodeConfigProvider = createNodeConfig(nodeIndex, nodeConfigFilename, nodesThisNodeKnowsAbout)
        //require(nodeConfigProvider.getConfiguration().activeChainIds.size == blockchainConfigFilenames.size) {
        //    "The nodes config must have the same number of active chains as the number of specified BC config files."
        //}
        return createMultipleChainNodeFromProvider(nodeConfigProvider, preWipeDatabase, *blockchainConfigFilenames)
    }

    private fun createMultipleChainNodeFromProvider(
            nodeConfigProvider: NodeConfigurationProvider,
            preWipeDatabase: Boolean = true,
            vararg blockchainConfigFilenames: String
    ): PostchainTestNode {

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

    /**
     * Using the latest way of configuring nodes, via exact configuration
     *
     * @param nodeSetup is the configuration object for the node
     * @param preWipeDatabase
     */
    /*
    private fun createMultipleChainNode(
            nodeSetup: NodeSetup,
            system: SystemSetup,
            preWipeDatabase: Boolean = true
    ): PostchainTestNode {
        val baseConfig: PropertiesConfiguration = createNodeConfig(
                testName: String,
                nodeSetup: TestNodeConfig,
                testSystemConfig: TestSystemConfig


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
*/

    protected fun readBlockchainConfig(blockchainConfigFilename: String): Gtv {
        return GtvMLParser.parseGtvML(
                javaClass.getResource(blockchainConfigFilename).readText())
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

    fun createPeerInfosWithReplicas(sysSetup: SystemSetup): Array<PeerInfo> {
        return sysSetup.toPeerInfoList().toTypedArray()
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
            val sigMaker = cryptoSystem.buildSigMaker(pubKey(i), privKey(i))
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