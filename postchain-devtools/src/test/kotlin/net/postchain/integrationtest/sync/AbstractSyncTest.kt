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
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.KeyPairHelper
import net.postchain.devtools.utils.configuration.BlockchainSetup
import net.postchain.devtools.utils.configuration.NodeSeqNumber
import net.postchain.devtools.utils.configuration.NodeSetup
import net.postchain.devtools.utils.configuration.SystemSetup
import net.postchain.devtools.utils.configuration.pre.BlockchainPreSetup
import net.postchain.network.x.XPeerID
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.MapConfiguration

open class AbstractSyncTest : IntegrationTestSetup() {

    protected fun runNodes(signerNodeCount: Int, replicaCount: Int): Array<NodeSetup> {

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

    protected fun restartNodeClean(nodeSetup: NodeSetup) {
        val nodeIndex = nodeSetup.sequenceNumber.nodeNumber
        val peerInfoMap = nodeSetup.configurationProvider!!.getConfiguration().peerInfoMap
        nodes[nodeIndex].shutdown()
        val newSetup = nodeSetup(nodeIndex, peerInfoMap.values.toTypedArray(), !nodeSetup.chainsToSign.isEmpty(), true)
        nodes[nodeIndex] = newSetup.toTestNodeAndStartAllChains(systemSetup, false)
    }

    protected fun startOldNode(nodeIndex: Int, peerInfoMap: Map<XPeerID, PeerInfo>, nodeSetup: NodeSetup) {
        val newSetup = nodeSetup(nodeIndex, peerInfoMap.values.toTypedArray(), !nodeSetup.chainsToSign.isEmpty(), false)
        nodes[nodeIndex] = newSetup.toTestNodeAndStartAllChains(systemSetup, false)
    }

    private fun nodeSetup(nodeIndex: Int, peerInfos: Array<PeerInfo>, isSigner: Boolean, wipeDb: Boolean): NodeSetup {
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

    private fun nodeConfigurationMap(nodeIndex: Int, peerInfo: PeerInfo): Configuration {
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
}