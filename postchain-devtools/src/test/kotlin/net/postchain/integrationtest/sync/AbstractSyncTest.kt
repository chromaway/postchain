package net.postchain.integrationtest.sync

import net.postchain.StorageBuilder
import net.postchain.base.BlockchainRid
import net.postchain.base.PeerInfo
import net.postchain.base.Storage
import net.postchain.base.data.DatabaseAccess
import net.postchain.base.data.DatabaseAccessFactory
import net.postchain.base.runStorageCommand
import net.postchain.common.toHex
import net.postchain.config.app.AppConfig
import net.postchain.config.node.ManagedNodeConfigurationProvider
import net.postchain.core.AppContext
import net.postchain.core.NODE_ID_NA
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.KeyPairHelper
import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.awaitHeight
import net.postchain.devtools.utils.configuration.BlockchainSetup
import net.postchain.devtools.utils.configuration.NodeSeqNumber
import net.postchain.devtools.utils.configuration.NodeSetup
import net.postchain.devtools.utils.configuration.SystemSetup
import net.postchain.devtools.utils.configuration.pre.BlockchainPreSetup
import net.postchain.network.x.XPeerID
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.MapConfiguration
import org.junit.Assert.assertArrayEquals

open class AbstractSyncTest : IntegrationTestSetup() {
    var signerCount: Int = -1
    open var mustSyncUntil = -1L


    protected fun runNodes(signerNodeCount: Int, replicaCount: Int): Array<NodeSetup> {
        signerCount = signerNodeCount
        val peerInfos = createPeerInfosWithReplicas(signerNodeCount, replicaCount)

        val chainId = 0
        val blockchainPreSetup = BlockchainPreSetup.simpleBuild(chainId, (0 until signerNodeCount).map { NodeSeqNumber(it) })
        val blockchainSetup = BlockchainSetup.buildFromGtv(chainId, blockchainPreSetup.toGtvConfig(mapOf()))
        var i = 0
        val nodeSetups = peerInfos.associate { NodeSeqNumber(i) to nodeSetup(i++, peerInfos, true, blockchainSetup.rid) }

        val systemSetup = SystemSetup(nodeSetups, mapOf(chainId to blockchainSetup), true,
                "legacy", "unused", "base/ebft", true)

        createNodesFromSystemSetup(systemSetup)
        return nodeSetups.values.toTypedArray()
    }

    val DEFAULT_STORAGE_FACTORY: (AppConfig) -> Storage = {
        StorageBuilder.buildStorage(it, NODE_ID_NA, false)
    }

    /** This function is used instead of the default one, to prepare the local database tables before node is started.
     * Byintroducing a prepareBlockchainOnNode function, preparations is separeted from the running. Thereby (as in this
     * example, we can populate the database table must_sync_until)
     **/
    open fun prepareBlockchainOnNode(setup: BlockchainSetup, node: PostchainTestNode) {
        node.addBlockchain(setup)
        node.mapBlockchainRID(setup.chainId.toLong(), setup.rid)
        node.setMustSyncUntil(setup.chainId.toLong(), setup.rid, mustSyncUntil)
    }

    protected fun restartNodeClean(nodeSetup: NodeSetup, brid: BlockchainRid) {
        val nodeIndex = nodeSetup.sequenceNumber.nodeNumber
        val peerInfoMap = nodeSetup.configurationProvider!!.getConfiguration().peerInfoMap
        nodes[nodeIndex].shutdown()
        val newSetup = nodeSetup(nodeIndex, peerInfoMap.values.toTypedArray(), true, brid)
        val blockchainSetup = systemSetup.blockchainMap[0]
        blockchainSetup!!.prepareBlockchainOnNode = { setup, node -> prepareBlockchainOnNode(setup, node) }
        nodes[nodeIndex] = newSetup.toTestNodeAndStartAllChains(systemSetup, false)
    }

    protected fun startOldNode(nodeIndex: Int, peerInfoMap: Map<XPeerID, PeerInfo>, brid: BlockchainRid) {
        val newSetup = nodeSetup(nodeIndex, peerInfoMap.values.toTypedArray(), false, brid)
        nodes[nodeIndex] = newSetup.toTestNodeAndStartAllChains(systemSetup, false)
    }

    private fun nodeSetup(nodeIndex: Int, peerInfos: Array<PeerInfo>, wipeDb: Boolean, brid: BlockchainRid): NodeSetup {
        val appConfig = AppConfig(nodeConfigurationMap(nodeIndex, peerInfos[nodeIndex]))
        val signer = if (nodeIndex < signerCount) setOf(0) else setOf()
        val replica = if (nodeIndex >= signerCount) setOf(0) else setOf()
        val nodeSetup = NodeSetup(NodeSeqNumber(nodeIndex), signer, replica, KeyPairHelper.pubKeyHex(nodeIndex),
                KeyPairHelper.privKeyHex(nodeIndex), ManagedNodeConfigurationProvider(appConfig, DEFAULT_STORAGE_FACTORY))
        StorageBuilder.buildStorage(appConfig, nodeIndex, wipeDb).close()

        if (wipeDb) {
            runStorageCommand(appConfig) {
                val ctx = it
                val dbAccess = DatabaseAccessFactory.createDatabaseAccess(appConfig.databaseDriverclass)
                peerInfos.forEachIndexed { index, peerInfo ->
                    val isPeerSigner = index < signerCount
                    addPeerInfo(dbAccess, ctx, peerInfo, brid, isPeerSigner)
                }
            }
        }
        return nodeSetup
    }

    open protected fun addPeerInfo(dbAccess: DatabaseAccess, ctx: AppContext, peerInfo: PeerInfo, brid: BlockchainRid, isPeerSigner: Boolean) {
        dbAccess.addPeerInfo(ctx, peerInfo)
        if (!isPeerSigner) {
            dbAccess.addBlockchainReplica(ctx, brid.toHex(), peerInfo.pubKey.toHex())
        }
    }

    //Faked node.properties file.
    fun nodeConfigurationMap(nodeIndex: Int, peerInfo: PeerInfo): Configuration {
        val privKey = KeyPairHelper.privKey(peerInfo.pubKey)
        return MapConfiguration(mapOf(
                "database.driverclass" to "org.postgresql.Driver",
                "database.url" to "jdbc:postgresql://localhost:5432/postchain",
                "database.username" to "postchain",
                "database.password" to "postchain",
                "database.schema" to this.javaClass.simpleName.toLowerCase() + "_$nodeIndex",
                "messaging.pubkey" to peerInfo.pubKey.toHex(),
                "messaging.privkey" to privKey.toHex(),
                "fastsync.exit_delay" to 2000, // All tests are multinode, see FastSyncParameters.exitDelay
                "api.port" to "-1"))
    }

    private fun createNodesFromSystemSetup(
            sysSetup: SystemSetup
    ) {
        this.systemSetup = sysSetup

        for (nodeSetup in systemSetup.nodeMap.values) {
            val newPTNode = nodeSetup.toTestNodeAndStartAllChains(systemSetup, false)
            nodes.add(newPTNode)
            nodeMap[nodeSetup.sequenceNumber] = newPTNode
        }
    }

    private fun n(index: Int): String {
        val p = nodes[index].pubKey
        return p.substring(0, 4) + ":" + p.substring(64)
    }

    /**
     * @param syncIndex which nodes to clean+restart and try to sync without help from stop index nodes
     * @param stopIndex which nodes to stop
     * @param blocksToSync height when sync nodes are wiped.
     */
    fun runSyncTest(signerCount: Int, replicaCount: Int, syncIndex: Set<Int>, stopIndex: Set<Int>, blocksToSync: Int) {
        val nodeSetups = runNodes(signerCount, replicaCount)
        val blockchainRid = nodes[0].getBlockchainRid(0)!!
        logger.debug { "All nodes started" }
        buildBlock(0, blocksToSync - 1L)
        logger.debug { "All nodes have block ${blocksToSync - 1}" }

        val expectedBlockRid = nodes[0].blockQueries(0).getBlockRid(blocksToSync - 1L).get()
        val peerInfos = nodeSetups[0].configurationProvider!!.getConfiguration().peerInfoMap
        stopIndex.forEach {
            logger.debug { "Shutting down ${n(it)}" }
            nodes[it].shutdown()
            logger.debug { "Shutting down ${n(it)} done" }
        }
        syncIndex.forEach {
            logger.debug { "Restarting clean ${n(it)}" }
            restartNodeClean(nodeSetups[it], blockchainRid)
            logger.debug { "Restarting clean ${n(it)} done" }
        }

        syncIndex.forEach {
            logger.debug { "Awaiting height ${blocksToSync - 1L} on ${n(it)}" }
            nodes[it].awaitHeight(0, blocksToSync - 1L)
            val actualBlockRid = nodes[it].blockQueries(0).getBlockRid(blocksToSync - 1L).get()
            assertArrayEquals(expectedBlockRid, actualBlockRid)
            logger.debug { "Awaiting height ${blocksToSync - 1L} on ${n(it)} done" }
        }

        stopIndex.forEach {
            logger.debug { "Start ${n(it)} again" }
            startOldNode(it, peerInfos, blockchainRid)
        }
        awaitHeight(0, blocksToSync - 1L)
        buildBlock(0, blocksToSync.toLong())
        logger.debug { "All nodes have block $blocksToSync" }
    }
}