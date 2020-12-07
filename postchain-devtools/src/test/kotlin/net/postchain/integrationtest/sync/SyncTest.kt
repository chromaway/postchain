package net.postchain.integrationtest.sync

import mu.KLogging
import net.postchain.StorageBuilder
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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SyncTest(val signerCount: Int, val replicaCount: Int, val syncIndex: Set<Int>, val stopIndex: Set<Int>, val blocksToSync: Int) : IntegrationTestSetup() {

    private companion object: KLogging() {

        @JvmStatic
        @Parameterized.Parameters
        fun testArguments() = listOf(
                // Single block test
                arrayOf(1, 1, setOf(0), setOf<Int>(), 1),
                arrayOf(1, 1, setOf(1), setOf<Int>(), 1),
                arrayOf(1, 2, setOf(1), setOf<Int>(0), 1),
                arrayOf(2, 0, setOf(1), setOf<Int>(), 1),

                // Multi block test
                arrayOf(1, 1, setOf(0), setOf<Int>(), 50),
                arrayOf(1, 1, setOf(1), setOf<Int>(), 50),
                arrayOf(1, 2, setOf(1), setOf<Int>(0), 50),
                arrayOf(2, 0, setOf(1), setOf<Int>(), 50),

                // Multi node multi blocks
                arrayOf(4, 4, setOf(0, 1, 2, 4, 5), setOf<Int>(3, 6), 50)
        )
    }

    /*
    The problem seems to be the same as in other p2p systems: How to find initial peers?

    Problems to solve:

    When a node (old or new) comes online it might not become aware of any currently
    active nodes because its nodelist might be outdated (network has evolved over time).

    It doesn’t have to happen at blockheight -1. This can happen at any height
    when the node has been offline for a while. This can only happen on chain0,
    because that's where all blockchain configurations and nodes are being tracked.
    As long as chain0 is fully synced, a node should be able to sync any other blockchain
    in that domain, because it has the most recent signerlist.

    Non-obvious tests to implement:


    1 Sync a managed blockchain (not chain0) from newest nodes, where none of the initial nodes exist. (Should work fine)
    2 Sync chain0 from newest nodes, where none of the initial nodes exist. Should work because newest nodes know about me.
    3 Test 2, but give it a syncnode via configuration (Should work)
    4 Sync single sole node from single nodelist (should wait for discovery timeout)

     */

    private fun n(index: Int): String {
        var p = nodes[index].pubKey
        return p.substring(0, 4) + ":" + p.substring(64)
    }

    @Test
    fun sync() {
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
            restartNodeClean(nodeSetups[it])
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
            startOldNode(it, peerInfos, nodeSetups[it])
        }
        buildBlock(0, blocksToSync.toLong())
        logger.debug { "All nodes has block $blocksToSync" }
    }

    private fun runNodes(signerNodeCount: Int, replicaCount: Int): Array<NodeSetup> {

        val peerInfos = createPeerInfosWithReplicas(signerNodeCount, replicaCount)

        var i = 0
        val nodeSetups = peerInfos.associate { NodeSeqNumber(i) to nodeSetup(i, peerInfos, i++<signerNodeCount, true) }

        val blockchainPreSetup = BlockchainPreSetup.simpleBuild(0, (0 until signerNodeCount).map { NodeSeqNumber(it) })
        val blockchainSetup = BlockchainSetup.buildFromGtv(0, blockchainPreSetup.toGtvConfig(mapOf()))

        val systemSetup = SystemSetup(nodeSetups, mapOf(0 to blockchainSetup), true, "managed", "managed", "base/ebft", true)

        createNodesFromSystemSetup(systemSetup)
        return nodeSetups.values.toTypedArray()
    }

    val DEFAULT_STORAGE_FACTORY: (AppConfig) -> Storage = {
        StorageBuilder.buildStorage(it, NODE_ID_NA, true)
    }

    fun restartNodeClean(nodeSetup: NodeSetup) {
        val nodeIndex = nodeSetup.sequenceNumber.nodeNumber
        val peerInfoMap = nodeSetup.configurationProvider!!.getConfiguration().peerInfoMap
        nodes[nodeIndex].shutdown()
        val newSetup = nodeSetup(nodeIndex, peerInfoMap.values.toTypedArray(), !nodeSetup.chainsToSign.isEmpty(), true)
        nodes[nodeIndex] = newSetup.toTestNodeAndStartAllChains(systemSetup, false)
    }

    private fun startOldNode(nodeIndex: Int, peerInfoMap: Map<XPeerID, PeerInfo>, nodeSetup: NodeSetup) {
        val newSetup = nodeSetup(nodeIndex, peerInfoMap.values.toTypedArray(), !nodeSetup.chainsToSign.isEmpty(), false)
        nodes[nodeIndex] = newSetup.toTestNodeAndStartAllChains(systemSetup, false)
    }

    fun nodeSetup(nodeIndex: Int, peerInfos: Array<PeerInfo>, isSigner: Boolean, wipeDb: Boolean): NodeSetup {
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

    fun createNodesFromSystemSetup(
            sysSetup: SystemSetup
    ) {
        this.systemSetup = sysSetup

        for (nodeSetup in systemSetup.nodeMap.values) {
            val newPTNode = nodeSetup.toTestNodeAndStartAllChains(systemSetup, false)

            // TODO: not nice to mutate the "nodes" object like this, should return the list of PTNodes instead for testability
            nodes.add(newPTNode)
            nodeMap[nodeSetup.sequenceNumber] = newPTNode
        }
    }
}