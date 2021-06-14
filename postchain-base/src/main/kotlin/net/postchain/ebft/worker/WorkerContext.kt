package net.postchain.ebft.worker

import net.postchain.base.PeerCommConfiguration
import net.postchain.config.node.NodeConfig
import net.postchain.core.BlockchainEngine
import net.postchain.debug.BlockchainProcessName
import net.postchain.ebft.message.Message
import net.postchain.network.CommunicationManager

/**
 * This is a transitional class to hide away clutter from EBFTSynchronizationInfrastructure. There's
 * room for improvement here, for example, peerCommComnfiguration is already part of the
 * communicationManager (If it's a DefaultXCommunicationManager, and so on).
 */
class WorkerContext(val processName: BlockchainProcessName,
                    val signers: List<ByteArray>,
                    val engine: BlockchainEngine,
                    val nodeId: Int, // Rename to signerIndex
                    val communicationManager: CommunicationManager<Message>,
                    val peerCommConfiguration: PeerCommConfiguration,
                    val nodeConfig: NodeConfig,
                    val onShutdown: () -> Unit,
                    val startWithFastSync: Boolean = true // for ValidatorWorker
) {
    fun shutdown() {
        engine.shutdown()
        communicationManager.shutdown()
        onShutdown()
    }
}