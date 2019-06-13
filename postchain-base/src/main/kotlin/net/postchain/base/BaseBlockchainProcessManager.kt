package net.postchain.base

import mu.KLogging
import net.postchain.StorageBuilder
import net.postchain.base.data.DatabaseAccess
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.*
import net.postchain.devtools.PeerNameHelper.peerName
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BaseBlockchainProcessManager(
        private val blockchainInfrastructure: BlockchainInfrastructure,
        private val nodeConfigProvider: NodeConfigurationProvider,
        private val blockchainConfigProvider: BlockchainConfigurationProvider
) : BlockchainProcessManager {

    val nodeConfig = nodeConfigProvider.getConfiguration()
    val storage = StorageBuilder.buildStorage(nodeConfig, NODE_ID_TODO)
    private val blockchainProcesses = mutableMapOf<Long, BlockchainProcess>()
    private val executor = Executors.newSingleThreadExecutor()

    companion object : KLogging()

    override fun startBlockchain(chainId: Long) {
        stopBlockchain(chainId)

        logger.info("Node ${buildPeerName()}: Starting of BlockchainProcess $chainId")

        withReadConnection(storage, chainId) { eContext ->
            val configuration = blockchainConfigProvider.getConfiguration(chainId)
            if (configuration != null) {
                val blockchainRID = DatabaseAccess.of(eContext).getBlockchainRID(eContext)!! // TODO: [et]: Fix Kotlin NPE
                val context = BaseBlockchainContext(blockchainRID, NODE_ID_AUTO, chainId, null)
                val blockchainConfig = blockchainInfrastructure.makeBlockchainConfiguration(configuration, context)

                val engine = blockchainInfrastructure.makeBlockchainEngine(blockchainConfig)
                val name = buildPeerName()
                blockchainProcesses[chainId] = blockchainInfrastructure.makeBlockchainProcess(name, engine) {
                    executor.execute {
                        startBlockchain(chainId)
                    }
                }
                logger.info("startBlockchain() - end")
            } else {
                println("Can't start blockchain due to configuration is absent")
            }

            Unit
        }
    }

    override fun retrieveBlockchain(chainId: Long): BlockchainProcess? {
        return blockchainProcesses[chainId]
    }

    override fun stopBlockchain(chainId: Long) {
        blockchainProcesses.remove(chainId)?.also {
            logger.info("Node ${buildPeerName()}: Stopping of BlockchainProcess $chainId")
            it.shutdown()
        }
    }

    override fun shutdown() {
        executor.shutdownNow()
        executor.awaitTermination(1000, TimeUnit.MILLISECONDS)
        blockchainProcesses.forEach { _, process -> process.shutdown() }
        blockchainProcesses.clear()
        storage.close()
        blockchainInfrastructure.shutdown()
    }

    private fun buildPeerName(): String {
        return peerName(nodeConfig.pubKey)
    }
}