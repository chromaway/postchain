package net.postchain.integrationtest.sync

import mu.KLogging
import net.postchain.StorageBuilder
import net.postchain.base.BlockchainRid
import net.postchain.base.PeerInfo
import net.postchain.base.data.DatabaseAccessFactory
import net.postchain.base.runStorageCommand
import net.postchain.common.toHex
import net.postchain.config.app.AppConfig
import net.postchain.config.node.ManagedNodeConfigurationProvider
import net.postchain.devtools.KeyPairHelper
import net.postchain.devtools.utils.configuration.NodeSeqNumber
import net.postchain.devtools.utils.configuration.NodeSetup
import org.awaitility.Awaitility
import org.junit.Test
import java.util.concurrent.TimeUnit

/* One signer, two replica nodes. After one block, node 0 (signer is turned off).
 * Node 1 (a replica) is wiped. Need node 2 (the other replica) to be able to sync.
 */
class ReplicaSyncTest : AbstractSyncTest() {

    private companion object: KLogging()
    private var removeAndAddAgain = false


    // Try to synchronize when the replica nodes have been removed from the blockchain_replicas table.
    @Test(expected = org.awaitility.core.ConditionTimeoutException::class)
    fun testRemove() {
        Awaitility.await().atMost(7, TimeUnit.SECONDS).until {
            doStuff(1, 2, setOf(1), setOf(0), 1)
            true
        }
    }

    // Check that sync problem is solved if nodes are added to the blockchain replica table again
    @Test
    fun testRemoveAndAddAgain() {
        removeAndAddAgain = true
        doStuff(1, 2, setOf(1), setOf(0), 1)
    }

    override fun nodeSetup(nodeIndex: Int, peerInfos: Array<PeerInfo>, isSigner: Boolean, wipeDb: Boolean, brid: BlockchainRid): NodeSetup {
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
                    dbAccess.removeBlockchainReplica(ctx, brid.toHex(), it.pubKey.toHex())
                }
                if (!isSigner && removeAndAddAgain) {
                    dbAccess.addBlockchainReplica(ctx, brid.toHex(), it.pubKey.toHex())
                }
            }
        }
        return nodeSetup
    }
}