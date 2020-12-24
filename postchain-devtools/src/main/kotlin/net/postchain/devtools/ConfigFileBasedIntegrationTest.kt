package net.postchain.devtools

import mu.KLogging
import net.postchain.StorageBuilder
import net.postchain.base.PeerInfo
import net.postchain.config.app.AppConfig
import net.postchain.config.node.NodeConfig
import net.postchain.config.node.NodeConfigurationProviderFactory
import net.postchain.core.*
import net.postchain.devtools.testinfra.TestTransaction
import net.postchain.devtools.utils.configuration.NodeNameWithBlockchains
import net.postchain.devtools.utils.configuration.UniversalFileLocationStrategy
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory
import org.apache.commons.configuration2.CompositeConfiguration
import org.apache.commons.configuration2.MapConfiguration
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler
import org.junit.After
import org.junit.Assert
import java.io.File

// Legacy code still use this old name, don't want to break compatibility.
typealias IntegrationTest = ConfigFileBasedIntegrationTest

/**
 * This is the integration test base class used before the Setup classes were created,
 * now most tests should go with the [IntegrationTestSetup] or [GtxTxIntegrationTestSetup]
 * We should still use this class for tests when we need to test broken configuration files,
 * or when we need to do non-standard stuff, like adding one blockchain at a time.
 */
open class ConfigFileBasedIntegrationTest : AbstractIntegration() {

    protected val nodes = mutableListOf<PostchainTestNode>()
    protected val nodesNames = mutableMapOf<String, String>() // { pubKey -> Node${i} }
    val configOverrides = MapConfiguration(mutableMapOf<String, String>())
    var gtxConfig: Gtv? = null

    // PeerInfos must be shared between all nodes because
    // a listening node will update the PeerInfo port after
    // ServerSocket is created.
    private var peerInfos: Array<PeerInfo>? = null
    private var expectedSuccessRids = mutableMapOf<Long, MutableList<ByteArray>>()

    private var txCounter = 0

    companion object : KLogging() {
        const val DEFAULT_CONFIG_FILE = "config.properties"
    }

    @After
    override fun tearDown() {
        try {
            logger.debug("Integration test -- TEARDOWN")
            nodes.forEach { it.shutdown() }
            nodes.clear()
            nodesNames.clear()
            logger.debug("Closed nodes")
            peerInfos = null
            expectedSuccessRids = mutableMapOf()
            configOverrides.clear()
            logger.debug("teadDown() done")
        } catch (t: Throwable) {
            logger.error("tearDown() failed", t)
        }
    }

    protected fun nextTx(): TestTransaction {
        return TestTransaction(txCounter++)
    }

    // TODO: [et]: Check out nullability for return value
    protected fun enqueueTx(node: PostchainTestNode, data: ByteArray, expectedConfirmationHeight: Long): Transaction? {
        val blockchainEngine = node.getBlockchainInstance().getEngine()
        val tx = blockchainEngine.getConfiguration().getTransactionFactory().decodeTransaction(data)
        enqueueTransactions(node, tx)

        if (expectedConfirmationHeight >= 0) {
            expectedSuccessRids.getOrPut(expectedConfirmationHeight) { mutableListOf() }
                    .add(tx.getRID())
        }

        return tx
    }

    protected fun enqueueTransactions(node: PostchainTestNode, vararg txs: Transaction) {
        val txQueue = node.getBlockchainInstance().getEngine().getTransactionQueue()
        txs.forEach { txQueue.enqueue(it) }
    }

    protected fun verifyBlockchainTransactions(node: PostchainTestNode) {
        val expectAtLeastHeight = expectedSuccessRids.keys.reduce { acc, l -> maxOf(l, acc) }
        val bestHeight = getBestHeight(node)
        Assert.assertTrue(bestHeight >= expectAtLeastHeight)
        for (height in 0..bestHeight) {
            val txRidsAtHeight = getTxRidsAtHeight(node, height)

            val expectedRidsAtHeight = expectedSuccessRids[height]
            if (expectedRidsAtHeight == null) {
                Assert.assertArrayEquals(arrayOf(), txRidsAtHeight)
            } else {
                Assert.assertArrayEquals(expectedRidsAtHeight.toTypedArray(), txRidsAtHeight)
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
            nodeConfigFilename: String,
            blockchainConfigFilename: String,
            preWipeDatabase: Boolean = true,
            setupAction: (appConfig: AppConfig, nodeConfig: NodeConfig) -> Unit = { _, _ -> Unit }
    ): PostchainTestNode {

        val appConfig = createAppConfig(nodeIndex, totalNodesCount, nodeConfigFilename)
        // Wiping of database
        if (preWipeDatabase) {
            StorageBuilder.buildStorage(appConfig, NODE_ID_TODO, true).close()
        }
        val nodeConfigProvider = NodeConfigurationProviderFactory.createProvider(appConfig)
        val nodeConfig = nodeConfigProvider.getConfiguration()

        nodesNames[nodeConfig.pubKey] = "$nodeIndex"
        val blockchainConfig = readBlockchainConfig(blockchainConfigFilename)
        val chainId = nodeConfig.activeChainIds.first().toLong()

        // Performing setup action
        setupAction(appConfig, nodeConfig)

        return PostchainTestNode(nodeConfigProvider)
                .apply {
                    val blockchainRid = addBlockchain(chainId, blockchainConfig)
                    mapBlockchainRID(chainId, blockchainRid)
                    startBlockchain(chainId)
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
     * @param count is the number of nodes
     * @param nodeConfigsFilenamesAndBlockchainConfigsFilenames an array with pairs, mapping the node to the actual blockchain file paths to run on this node.
     */
    protected fun createMultipleChainNodesWithVariableNumberOfChains(
            count: Int,
            nodeNameWithBlockchainsArr: Array<NodeNameWithBlockchains>
    ): Array<PostchainTestNode> {

        require(count == nodeNameWithBlockchainsArr.size) { "Must have as many nodes in the array as specified" }

        return Array(count) {
            val bcFilenames: List<String> = nodeNameWithBlockchainsArr[it].getFilenames()
            createMultipleChainNode(it, count, nodeNameWithBlockchainsArr[it].nodeFileName, *(bcFilenames.toTypedArray()))
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

        val appConfig = createAppConfig(nodeIndex, nodeCount, nodeConfigFilename)
        // Wiping of database
        if (preWipeDatabase) {
            StorageBuilder.buildStorage(appConfig, NODE_ID_TODO, true).close()
        }
        val nodeConfigProvider = NodeConfigurationProviderFactory.createProvider(appConfig)

        val node = PostchainTestNode(nodeConfigProvider)
                .also { nodes.add(it) }

        nodeConfigProvider.getConfiguration().activeChainIds
                .filter(String::isNotEmpty)
                .forEachIndexed { i, chainId ->
                    val filename = blockchainConfigFilenames[i]
                    val blockchainConfig = readBlockchainConfig(filename)
                    val blockchainRid = node.addBlockchain(chainId.toLong(), blockchainConfig)
                    node.mapBlockchainRID(chainId.toLong(), blockchainRid)
                    node.startBlockchain(chainId.toLong())
                }

        return node
    }

    protected fun createAppConfig(nodeIndex: Int, nodeCount: Int = 1, configFile /*= DEFAULT_CONFIG_FILE*/: String)
            : AppConfig {

        val file = File(configFile)

        // Read first file directly via the builder
        val params = Parameters()
                .fileBased()
                .setLocationStrategy(UniversalFileLocationStrategy())
                .setListDelimiterHandler(DefaultListDelimiterHandler(','))
                .setFile(file)

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
                baseConfig.setProperty("node.$i.pubkey", KeyPairHelper.pubKeyHex(i))
            }

            baseConfig.setProperty("messaging.privkey", KeyPairHelper.privKeyHex(nodeIndex))
            baseConfig.setProperty("messaging.pubkey", KeyPairHelper.pubKeyHex(nodeIndex))
        }

        if (nodeCount == 1) {
            // We only have one node. Skip time consuming node discovery
            // in FastSynchronizer
            baseConfig.setProperty("fastsync.discovery_timeout", 0) // ms
        }

        val appConfig = CompositeConfiguration().apply {
            addConfiguration(configOverrides)
            addConfiguration(baseConfig)
        }

        return AppConfig(appConfig)
    }

    protected fun gtxConfigSigners(nodeCount: Int = 1): Gtv {
        return GtvFactory.gtv(*Array(nodeCount) { GtvFactory.gtv(KeyPairHelper.pubKey(it)) })
    }

    open fun generatePubKey(nodeId: Int): ByteArray = KeyPairHelper.pubKey(nodeId)

    fun createPeerInfosWithReplicas(nodeCount: Int, replicasCount: Int): Array<PeerInfo> {
        if (peerInfos == null) {
            peerInfos =
                    Array(nodeCount) { PeerInfo("localhost", BASE_PORT + it, generatePubKey(it)) } +
                            Array(replicasCount) { PeerInfo("localhost", BASE_PORT - it - 1, generatePubKey(-it - 1)) }
        }

        return peerInfos!!
    }

    open fun createPeerInfos(nodeCount: Int): Array<PeerInfo> = createPeerInfosWithReplicas(nodeCount, 0)

    protected fun strategy(node: PostchainTestNode): OnDemandBlockBuildingStrategy {
        return node.getBlockchainInstance().getEngine().getBlockBuildingStrategy() as OnDemandBlockBuildingStrategy
    }

    protected fun buildBlock(toHeight: Int, vararg txs: TestTransaction) {
        nodes.forEach {
            enqueueTransactions(it, *txs)
            strategy(it).buildBlocksUpTo(toHeight.toLong())
        }
        nodes.forEach {
            strategy(it).awaitCommitted(toHeight)
        }
    }

    protected fun buildNonEmptyBlocks(fromHeight: Int, toHeight: Int) {
        repeat(toHeight - fromHeight) { c ->
            val tx = nextTx()
            buildBlock(fromHeight + 1 + c, tx)
        }
    }


}