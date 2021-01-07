package net.postchain.integrationtest.sync

import net.postchain.StorageBuilder
import net.postchain.base.BlockchainRid
import net.postchain.base.PeerInfo
import net.postchain.base.Storage
import net.postchain.base.data.DatabaseAccessFactory
import net.postchain.base.runStorageCommand
import net.postchain.common.toHex
import net.postchain.config.app.AppConfig
import net.postchain.config.node.ManagedNodeConfigurationProvider
import net.postchain.core.NODE_ID_NA
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.KeyPairHelper
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

    protected fun runNodes(signerNodeCount: Int, replicaCount: Int): Array<NodeSetup> {

        val peerInfos = createPeerInfosWithReplicas(signerNodeCount, replicaCount)

        val blockchainPreSetup = BlockchainPreSetup.simpleBuild(0, (0 until signerNodeCount).map { NodeSeqNumber(it) })
        val blockchainSetup = BlockchainSetup.buildFromGtv(0, blockchainPreSetup.toGtvConfig(mapOf()))
        var i = 0
        val nodeSetups = peerInfos.associate { NodeSeqNumber(i) to nodeSetup(i, peerInfos, i++<signerNodeCount, true, blockchainSetup.rid) }

        val systemSetup = SystemSetup(nodeSetups, mapOf(0 to blockchainSetup), true, "managed", "managed", "base/ebft", true)

        createNodesFromSystemSetup(systemSetup)
        return nodeSetups.values.toTypedArray()
    }

    val DEFAULT_STORAGE_FACTORY: (AppConfig) -> Storage = {
        StorageBuilder.buildStorage(it, NODE_ID_NA, true)
    }

    protected fun restartNodeClean(nodeSetup: NodeSetup, brid: BlockchainRid) {
        val nodeIndex = nodeSetup.sequenceNumber.nodeNumber
        val peerInfoMap = nodeSetup.configurationProvider!!.getConfiguration().peerInfoMap
        nodes[nodeIndex].shutdown()
        val newSetup = nodeSetup(nodeIndex, peerInfoMap.values.toTypedArray(), !nodeSetup.chainsToSign.isEmpty(), true, brid)
        nodes[nodeIndex] = newSetup.toTestNodeAndStartAllChains(systemSetup, false)
    }

    protected fun startOldNode(nodeIndex: Int, peerInfoMap: Map<XPeerID, PeerInfo>, nodeSetup: NodeSetup, brid: BlockchainRid) {
        val newSetup = nodeSetup(nodeIndex, peerInfoMap.values.toTypedArray(), !nodeSetup.chainsToSign.isEmpty(), false, brid)
        nodes[nodeIndex] = newSetup.toTestNodeAndStartAllChains(systemSetup, false)
    }

    open protected fun nodeSetup(nodeIndex: Int, peerInfos: Array<PeerInfo>, isSigner: Boolean, wipeDb: Boolean, brid: BlockchainRid): NodeSetup {
        val appConfig = AppConfig(nodeConfigurationMap(nodeIndex, peerInfos[nodeIndex]))
        val signer = if (isSigner) setOf(0) else setOf()
        val replica = if (isSigner) setOf() else setOf(0)
        val nodeSetup = NodeSetup(NodeSeqNumber(nodeIndex), signer, replica, KeyPairHelper.pubKeyHex(nodeIndex),
                KeyPairHelper.privKeyHex(nodeIndex), ManagedNodeConfigurationProvider(appConfig, DEFAULT_STORAGE_FACTORY))
        StorageBuilder.buildStorage(appConfig, nodeIndex, wipeDb).close()

        runStorageCommand(appConfig) {
            val ctx = it
            val dbAccess = DatabaseAccessFactory.createDatabaseAccess(appConfig.databaseDriverclass)
            peerInfos.forEach {
                dbAccess.addPeerInfo(ctx, it)
                if (!isSigner) {
                    dbAccess.addBlockchainReplica(ctx, brid.toHex(), it.pubKey.toHex())
                }
            }
        }
        return nodeSetup
    }

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
        var p = nodes[index].pubKey
        return p.substring(0, 4) + ":" + p.substring(64)
    }

    /**
     * @param stopIndex which nodes to stop
     * @param syncIndex which nodes to clean+restart and try to sync without help from stop index nodes
     * @param blocksToSync height when sync nodes are wiped.
     */
    fun doStuff(signerCount: Int, replicaCount: Int, syncIndex: Set<Int>, stopIndex: Set<Int>, blocksToSync: Int) {
        val nodeSetups = runNodes(signerCount, replicaCount)
        logger.debug { "All nodes started" }
        buildBlock(0, blocksToSync-1L)
        logger.debug { "All nodes have block ${blocksToSync-1}" }

        val expectedBlockRid = nodes[0].blockQueries(0).getBlockRid(blocksToSync-1L).get()
        val peerInfos = nodeSetups[0].configurationProvider!!.getConfiguration().peerInfoMap
        stopIndex.forEach {
            logger.debug { "Shutting down ${n(it)}" }
            nodes[it].shutdown()
            logger.debug { "Shutting down ${n(it)} done" }
        }
        syncIndex.forEach {
            logger.debug { "Restarting clean ${n(it)}" }
            restartNodeClean(nodeSetups[it], nodes[0].getBlockchainRid(0)!!)
            logger.debug { "Restarting clean ${n(it)} done" }
        }

        syncIndex.forEach {
            logger.debug { "Awaiting height 0 on ${n(it)}" }
            nodes[it].awaitHeight(0, blocksToSync-1L)
            val actualBlockRid = nodes[it].blockQueries(0).getBlockRid(blocksToSync-1L).get()
            assertArrayEquals(expectedBlockRid, actualBlockRid)
            logger.debug { "Awaiting height 0 on ${n(it)} done" }
        }

        stopIndex.forEach {
            logger.debug { "Start ${n(it)} again" }
            startOldNode(it, peerInfos, nodeSetups[it], nodes[0].getBlockchainRid(0)!!)
        }
        buildBlock(0, blocksToSync.toLong())
        logger.debug { "All nodes has block $blocksToSync" }
    }
}