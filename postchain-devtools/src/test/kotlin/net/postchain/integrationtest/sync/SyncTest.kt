package net.postchain.integrationtest.sync

import net.postchain.StorageBuilder
import net.postchain.base.PeerInfo
import net.postchain.base.Storage
import net.postchain.base.data.DatabaseAccessFactory
import net.postchain.base.runStorageCommand
import net.postchain.common.toHex
import net.postchain.config.app.AppConfig
import net.postchain.config.node.ManagedNodeConfigurationProvider
import net.postchain.core.NODE_ID_NA
import net.postchain.devtools.*
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

class SyncTest : IntegrationTestSetup() {


    /*
    The problem seems to be the same as in other p2p systems: How to find initial peers?

    Problems to solve:

    When a node (old or new) comes online it might not become aware of any currently
    active nodes due to sync from.

    It doesn’t have to happen at blockheight -1. This can happen at any height
    when the node has been offline for a while. This can only happen on chain0,
    because that's where all blockchain configurations and nodes are being tracked.
    As long as chain0 is fully synced, a node should be able to sync any other blockchain
    in that domain.

    I think we should do a manual approach, where we eg. provide a list of nodes
    to sync from (let’s call it synclist) at startup. For example

            ./postchain.sh -synclist 192.168.1.13:9878,192.168.1.14:9962 bla bla bla

    1. If synclist is provided the node will use that to catch up (It doesn’t
    care what pubkey that node uses).

    2. If no synclist is provided or previous step failed, the node will use
    whatever is in the db to catch up

    3. If there’s nothing in the db or previous step failed, and this node is the
    sole signer, it will start building blocks on its own.

    4. If it’s not a signer quit with error message.

    Note that if you sync from a replica, and a configuration change occurrs, and
    that new config is also outdated.

    I think it's a bit odd to have the list of nodes (ip, port) being part of consensus.
    It's only the keys that should be part of consensus. Where nodes are located is irrelevant.
    We should see this consensus-driven node list as a help in getting new nodes connected,
    but the nodes in that list should be overridable. Suppose there are currently 10 signing nodes,
    and you happen to know where 1 of them are.

    What to test?

    * A node connects to replicas
    * A replica connects to nodes
    * A replica connects to replica
     */

    fun syncSigner(signerCount: Int, replicaCount: Int, syncIndex: Int) {
        syncSigners(signerCount, replicaCount, setOf(syncIndex), setOf())
    }

    fun syncSigners(signerCount: Int, replicaCount: Int, syncIndex: Set<Int>, stopIndex: Set<Int> = setOf()) {
        val nodeSetups = runNodes(signerCount, replicaCount)

        buildBlock(0, 0)
        val expectedBlockRid = nodes[0].blockQueries(0).getBlockRid(0).get()

        val peerInfos = nodeSetups[0].configurationProvider!!.getConfiguration().peerInfoMap
        stopIndex.forEach {
            nodes[it].shutdown()
        }
        syncIndex.forEach {
            restartNodeClean(nodeSetups[it])
        }

        syncIndex.forEach {
            nodes[it].awaitHeight(0, 0)
            val actualBlockRid = nodes[it].blockQueries(0).getBlockRid(0).get()
            assertArrayEquals(expectedBlockRid, actualBlockRid)
        }

        stopIndex.forEach {
            startOldNode(it, peerInfos, nodeSetups[it])
        }
        buildBlock(0, 1)
    }

    @Test
    fun testSyncSignerFromReplica() {
        syncSigner(1, 1, 0)
    }

    @Test
    fun testSyncReplicaFromSigner() {
        syncSigner(1, 1, 1)
    }

    @Test
    fun testSyncReplicaFromReplica() {
        syncSigners(1, 2, setOf(1), setOf(0))
    }

    @Test
    fun testSyncSignerFromSigner() {
        syncSigner(2, 0, 1)
    }

    private fun runNodes(signerNodeCount: Int, replicaCount: Int): Array<NodeSetup> {

        val peerInfos = createPeerInfosWithReplicas(signerNodeCount, replicaCount)

        var i = 0
        val nodeSetups = peerInfos.associate { NodeSeqNumber(i) to nodeSetup(i, peerInfos, i++<signerNodeCount, true) }

        val blockchainPreSetup = BlockchainPreSetup.simpleBuild(0, listOf(NodeSeqNumber(0)))
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