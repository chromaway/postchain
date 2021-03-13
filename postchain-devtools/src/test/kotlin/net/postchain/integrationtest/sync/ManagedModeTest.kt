package net.postchain.integrationtest.sync

import mu.KLogging
import net.postchain.base.*
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.base.data.DatabaseAccess
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.NodeConfig
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.*
import net.postchain.debug.NodeDiagnosticContext
import net.postchain.devtools.*
import net.postchain.devtools.testinfra.TestTransactionFactory
import net.postchain.ebft.EBFTSynchronizationInfrastructure
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvDictionary
import net.postchain.integrationtest.sync.ManagedModeTest.NodeSet
import net.postchain.managed.ManagedBlockchainProcessManager
import net.postchain.managed.ManagedEBFTInfrastructureFactory
import net.postchain.managed.ManagedNodeDataSource
import net.postchain.network.x.XPeerID
import org.apache.commons.configuration2.Configuration
import java.lang.Thread.sleep
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import kotlin.test.assertTrue

open class ManagedModeTest : AbstractSyncTest() {

    private companion object: KLogging()
    val mockDataSources = mutableMapOf<Int, MockManagedNodeDataSource>()

    inner class NodeSet(val chain: Long, val signers: Set<Int>, val replicas: Set<Int>) {
        val size: Int = signers.size + replicas.size
        fun contains(i: Int) = signers.contains(i) || replicas.contains(i)
        fun all(): Set<Int> = signers.union(replicas)
        fun nodes() = nodes.filterIndexed { i, p -> contains(i) }
        /**
         * Creates a new NodeSet as a copy of this NodeSet, but
         * with some nodes removed
         */
        fun remove(nodesToRemove: Set<Int>): ManagedModeTest.NodeSet {
            return NodeSet(chain, signers.minus(nodesToRemove), replicas.minus(nodesToRemove))
        }
    }

    fun dataSources(nodeSet: NodeSet): Map<Int, MockManagedNodeDataSource> {
        return mockDataSources.filterKeys { nodeSet.contains(it) }
    }

    fun addBlockchainConfiguration(nodeSet: NodeSet, historicChain: Long?, height: Long) {
        val brid = chainRidOf(nodeSet.chain)

        val signerGtvs = mutableListOf<GtvByteArray>()
        if (nodeSet.chain == 0L) {
            nodeSet.signers.forEach {
                signerGtvs.add(GtvByteArray(KeyPairHelper.pubKey(it)))
            }
        } else {
            nodeSet.signers.forEach {
                signerGtvs.add(GtvByteArray(nodes[it].pubKey.hexStringToByteArray()))
            }
        }

        mockDataSources.forEach {
            val data = TestBlockchainConfigurationData()
            data.setValue(BaseBlockchainConfigurationData.KEY_SIGNERS, GtvArray(signerGtvs.toTypedArray()))
            if (historicChain != null) {
                data.setValue(BaseBlockchainConfigurationData.KEY_HISTORIC_BRID, GtvByteArray(chainRidOf(historicChain).data))
            }
            val pubkey = if (nodeSet.chain == 0L) {
                if (it.key < nodeSet.signers.size) {
                    KeyPairHelper.pubKey(it.key)
                } else {
                    KeyPairHelper.pubKey(-1 - it.key)
                }
            } else {
                nodes[it.key].pubKey.hexStringToByteArray()
            }

            val context = BaseBlockchainContext(brid, NODE_ID_AUTO, nodeSet.chain, pubkey)

            val privkey = KeyPairHelper.privKey(pubkey)
            val sigMaker = cryptoSystem.buildSigMaker(pubkey, privkey)
            val confData = BaseBlockchainConfigurationData(data.getDict(), context, sigMaker)
            val bcConf = TestBlockchainConfiguration(confData)
            it.value.addConf(brid, height, bcConf, nodeSet)
        }
    }

    fun setupDataSources(nodeSet: NodeSet) {
        for (i in 0 until nodeSet.size) {
            val dataSource = MockManagedNodeDataSource(i)
            mockDataSources.put(i, dataSource)
        }
        addBlockchainConfiguration(nodeSet, null, 0)
    }

    fun newBlockchainConfiguration(nodeSet: NodeSet, historicChain: Long?, height: Long, excludeChain0Nodes: Set<Int> = setOf()) {
        addBlockchainConfiguration(nodeSet, historicChain, height)
        // We need to build a block on c0 to trigger c0's restartHandler, otherwise
        // the node manager won't become aware of the new configuration
        buildBlock(c0.remove(excludeChain0Nodes))
    }

    protected fun awaitChainRunning(index: Int, chainId: Long, atLeastHeight: Long) {
        val pm = nodes[index].processManager as TestManagedBlockchainProcessManager
        pm.awaitStarted(chainId, atLeastHeight)
    }

    fun restartNodeClean(index: Int, nodeSet: NodeSet, atLeastHeight: Long) {
        restartNodeClean(index, chainRidOf(0))
        awaitChainRunning(index, nodeSet.chain, atLeastHeight)
    }

    fun buildBlock(nodeSet: NodeSet, toHeight: Long) {
        buildBlock(nodes.filterIndexed { i,p -> nodeSet.contains(i) }, nodeSet.chain.toLong(), toHeight)
    }

    fun buildBlock(nodeSet: NodeSet) {
        val currentHeight = nodeSet.nodes()[0].currentHeight(nodeSet.chain)
        buildBlock(nodeSet, currentHeight+1)
    }

    fun awaitHeight(nodeSet: NodeSet, height: Long) {
        awaitHeight(nodeSet.nodes(), nodeSet.chain, height)
    }

    fun assertCantBuildBlock(nodeSet: NodeSet, height: Long) {
        buildBlockNoWait(nodeSet.nodes(), nodeSet.chain, height)
        sleep(1000)
        nodeSet.nodes().forEach {
            assertTrue(it.blockQueries(nodeSet.chain).getBestHeight().get() < height)
        }
    }

    override fun nodeConfigurationMap(nodeIndex: Int, peerInfo: PeerInfo): Configuration {
        val propertyMap = super.nodeConfigurationMap(nodeIndex, peerInfo)
        var className = TestManagedEBFTInfrastructureFactory::class.qualifiedName
        propertyMap.setProperty("infrastructure", className)
        propertyMap.setProperty("infrastructure.datasource", mockDataSources[nodeIndex])
        return propertyMap
    }

    lateinit var c0: NodeSet
    fun startManagedSystem(signers: Int, replicas: Int) {
        c0 = NodeSet(0, (0 until signers).toSet(), (signers until signers+replicas).toSet())
        setupDataSources(c0)
        runNodes(c0.signers.size, c0.replicas.size)
        buildBlock(c0, 0)
    }


    class TestBlockchainConfigurationData {
        private val m = mutableMapOf<String, Gtv>()
        fun getDict(): GtvDictionary {
            return GtvDictionary.build(m)
        }

        fun setValue(key: String, value: Gtv) {
            m[key] = value
        }
    }


    class TestBlockchainConfiguration(data: BaseBlockchainConfigurationData):
            BaseBlockchainConfiguration(data) {
        override fun getTransactionFactory(): TransactionFactory {
            return TestTransactionFactory()
        }

        override fun getBlockBuildingStrategy(blockQueries: BlockQueries, txQueue: TransactionQueue): BlockBuildingStrategy {
            return OnDemandBlockBuildingStrategy(configData, this, blockQueries, txQueue)
        }
    }
}

class TestManagedEBFTInfrastructureFactory : ManagedEBFTInfrastructureFactory() {
    lateinit var nodeConfig: NodeConfig
    lateinit var dataSource: MockManagedNodeDataSource
    override fun makeProcessManager(nodeConfigProvider: NodeConfigurationProvider,
                                    blockchainInfrastructure: BlockchainInfrastructure,
                                    blockchainConfigurationProvider: BlockchainConfigurationProvider,
                                    nodeDiagnosticContext: NodeDiagnosticContext): BlockchainProcessManager {
        return TestManagedBlockchainProcessManager(blockchainInfrastructure, nodeConfigProvider,
                blockchainConfigurationProvider, nodeDiagnosticContext, dataSource)
    }

    override fun makeBlockchainInfrastructure(nodeConfigProvider: NodeConfigurationProvider,
                                              nodeDiagnosticContext: NodeDiagnosticContext): BlockchainInfrastructure {
        nodeConfig = nodeConfigProvider.getConfiguration()
        dataSource = nodeConfig.appConfig.config.get(MockManagedNodeDataSource::class.java, "infrastructure.datasource")!!

        val syncInfra = EBFTSynchronizationInfrastructure(nodeConfigProvider, nodeDiagnosticContext)
        val apiInfra = BaseApiInfrastructure(nodeConfigProvider, nodeDiagnosticContext)
        val infrastructure = TestManagedBlockchainInfrastructure(nodeConfigProvider, syncInfra, apiInfra, nodeDiagnosticContext, dataSource)
        return infrastructure
    }

    override fun makeBlockchainConfigurationProvider(): BlockchainConfigurationProvider {
        return TestBlockchainConfigurationProvider(dataSource)
    }
}


class TestBlockchainConfigurationProvider(val mockDataSource: ManagedNodeDataSource):
        BlockchainConfigurationProvider {
    override fun getConfiguration(eContext: EContext, chainId: Long): ByteArray? {
        val db = DatabaseAccess.of(eContext)
        val height = db.getLastBlockHeight(eContext)
        return mockDataSource.getConfiguration(chainRidOf(chainId).data, height+1)
    }

    override fun needsConfigurationChange(eContext: EContext, chainId: Long): Boolean {
        val dba = DatabaseAccess.of(eContext)
        val height = dba.getLastBlockHeight(eContext)
        val blockchainRid = chainRidOf(chainId)
        val nextConfigHeight = mockDataSource.findNextConfigurationHeight(blockchainRid.data, height)
        return (nextConfigHeight != null) && (nextConfigHeight == height + 1)
    }
}


class TestManagedBlockchainInfrastructure(nodeConfigProvider: NodeConfigurationProvider,
                                          syncInfra: SynchronizationInfrastructure, apiInfra: ApiInfrastructure,
                                          nodeDiagnosticContext: NodeDiagnosticContext, val mockDataSource: MockManagedNodeDataSource) :
        BaseBlockchainInfrastructure(nodeConfigProvider, syncInfra, apiInfra, nodeDiagnosticContext) {
    override fun makeBlockchainConfiguration(rawConfigurationData: ByteArray, eContext: EContext, nodeId: Int, chainId: Long): BlockchainConfiguration {
        return mockDataSource.getConf(rawConfigurationData)!!
    }
}

class TestManagedBlockchainProcessManager(blockchainInfrastructure: BlockchainInfrastructure,
                                          nodeConfigProvider: NodeConfigurationProvider,
                                          blockchainConfigProvider: BlockchainConfigurationProvider,
                                          nodeDiagnosticContext: NodeDiagnosticContext,
                                          val dataSource: ManagedNodeDataSource)
    : ManagedBlockchainProcessManager(blockchainInfrastructure,
        nodeConfigProvider,
        blockchainConfigProvider,
        nodeDiagnosticContext) {

    private val blockchainStarts = ConcurrentHashMap<Long, BlockingQueue<Long>>()

    override fun buildChain0ManagedDataSource(): ManagedNodeDataSource {
        return dataSource
    }

    override fun retrieveBlockchainsToLaunch(): Array<Long> {
        val result = mutableListOf<Long>()
        dataSource.computeBlockchainList().forEach {
            val brid = BlockchainRid(it)
            val chainIid = chainIidOf(brid)
            result.add(chainIid)
            withReadWriteConnection(storage, chainIid) { newCtx ->
                DatabaseAccess.of(newCtx).initializeBlockchain(newCtx, brid)
            }
            val i = 0
        }
        return result.toTypedArray()
    }

    private fun getQueue(chainId: Long): BlockingQueue<Long> {
        return blockchainStarts.computeIfAbsent(chainId) {
            LinkedBlockingQueue<Long>()
        }
    }

    var lastHeightStarted = ConcurrentHashMap<Long, Long>()
    override fun startBlockchain(chainId: Long): BlockchainRid? {
        val blockchainRid = super.startBlockchain(chainId)
        if (blockchainRid == null) {
            return null
        }
        val process = blockchainProcesses[chainId]!!
        val queries = process.getEngine().getBlockQueries()
        val height = queries.getBestHeight().get()
        lastHeightStarted[chainId] = height
        return blockchainRid
    }

    fun awaitStarted(chainId: Long, atLeastHeight: Long) {
        while (lastHeightStarted.get(chainId) ?: -2L < atLeastHeight) {
            sleep(10)
        }
    }
}

fun chainRidOf(chainIid: Long): BlockchainRid {
    val hexChainIid = chainIid.toString(8)
    val base = "0000000000000000000000000000000000000000000000000000000000000000"
    val rid = base.substring(0, 64-hexChainIid.length) + hexChainIid
    return BlockchainRid.buildFromHex(rid)
}

fun chainIidOf(brid: BlockchainRid): Long {
    return brid.toHex().toLong(8)
}

typealias Key = Pair<BlockchainRid, Long>



class MockManagedNodeDataSource(val nodeIndex: Int) : ManagedNodeDataSource {
    // Brid -> (height -> Pair<BlockchainConfiguration, NodeSet>)
    private val bridToConfs: MutableMap<BlockchainRid, MutableMap<Long, BlockchainConfiguration>> = mutableMapOf()
    private val chainToNodeSet: MutableMap<BlockchainRid, NodeSet> = mutableMapOf()
    private val extraReplicas = mutableMapOf<BlockchainRid, MutableSet<XPeerID>>()

    override fun getPeerListVersion(): Long {
        return 1L
    }

    override fun computeBlockchainList(): List<ByteArray> {
        return chainToNodeSet.filterValues { it.contains(nodeIndex) }.keys.map { it.data }
    }

    override fun getConfiguration(blockchainRIDRaw: ByteArray, height: Long): ByteArray? {
        val l = bridToConfs[BlockchainRid(blockchainRIDRaw)] ?: return null
        var conf: ByteArray? = null
        for (entry in l) {
            if (entry.key <= height) {
                conf = toByteArray(Key(BlockchainRid(blockchainRIDRaw), entry.key))
            } else {
                return conf
            }
        }
        return conf
    }

    override fun findNextConfigurationHeight(blockchainRIDRaw: ByteArray, height: Long): Long? {
        val l = bridToConfs[BlockchainRid(blockchainRIDRaw)] ?: return null
        for (h in l.keys) {
            if (h > height) {
                return h
            }
        }
        return null
    }

    override fun getPeerInfos(): Array<PeerInfo> {
        return emptyArray()
    }

    override fun getSyncUntilHeight(): Map<BlockchainRid, Long> {
        return emptyMap()
    }

    override fun getNodeReplicaMap(): Map<XPeerID, List<XPeerID>> {
        return mapOf()
    }

    override fun getBlockchainReplicaNodeMap(): Map<BlockchainRid, List<XPeerID>> {
        val result = mutableMapOf<BlockchainRid, List<XPeerID>>()
        chainToNodeSet.keys.union(extraReplicas.keys).forEach {
            val replicaSet = chainToNodeSet[it]?.replicas ?: emptySet()
            var replicas = replicaSet.map { XPeerID(KeyPairHelper.pubKey(it)) }.toMutableSet()
            replicas.addAll(extraReplicas[it] ?: emptySet())
            result.put(it, replicas.toList())
        }
        return result
    }

    fun addExtraReplica(brid: BlockchainRid, replica: XPeerID) {
        extraReplicas.computeIfAbsent(brid) { mutableSetOf<XPeerID>() }.add(replica)
    }

    private fun key(brid: BlockchainRid, height: Long): Key {
        return Pair(brid, height)
    }

    private fun toByteArray(key: Key): ByteArray {
        var heightHex = key.second.toString(8)
        if (heightHex.length % 2 == 1) {
            heightHex = "0" + heightHex
        }
        return (key.first.toHex() + heightHex).hexStringToByteArray()
    }

    private fun toKey(bytes: ByteArray): Key {
        val rid = BlockchainRid(bytes.copyOf(32))
        val height =  bytes.copyOfRange(32, bytes.size).toHex().toLong(8)
        return Key(rid, height)
    }

    fun getConf(bytes: ByteArray): BlockchainConfiguration? {
        val key = toKey(bytes)
        return bridToConfs[key.first]?.get(key.second)
    }

    fun addConf(rid: BlockchainRid, height: Long, conf: BlockchainConfiguration, nodeSet: NodeSet) {
        val confs = bridToConfs.computeIfAbsent(rid) { sortedMapOf()}
        if (confs!!.put(height, conf) != null) {
            throw IllegalArgumentException("Setting blockchain configuraion for height that already has a configuration")
        }
        chainToNodeSet.put(chainRidOf(nodeSet.chain), nodeSet)
    }

    /**
     * This is to force a node to become totally unaware of a certain blockchain.
     */
    fun delBlockchain(rid: BlockchainRid) {
        bridToConfs.remove(rid)
        extraReplicas.remove(rid)
        chainToNodeSet.remove(rid)
    }
}