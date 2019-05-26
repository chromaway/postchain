package net.postchain.base

import mu.KLogging
import net.postchain.StorageBuilder
import net.postchain.base.data.DatabaseAccess
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

open class BaseBlockchainProcessManager(
        protected val blockchainInfrastructure: BlockchainInfrastructure,
        protected val nodeConfigProvider: NodeConfigurationProvider,
        protected val blockchainConfigProvider: BlockchainConfigurationProvider
) : BlockchainProcessManager {

    val storage = StorageBuilder.buildStorage(nodeConfigProvider.getConfiguration(), NODE_ID_TODO)
    private val blockchainProcesses = mutableMapOf<Long, BlockchainProcess>()
    protected val executor = Executors.newSingleThreadScheduledExecutor()

    companion object: KLogging()

    override fun startBlockchainAsync(chainId: Long) {
        executor.execute {
            startBlockchain(chainId)
        }
    }

    override fun startBlockchain(chainId: Long) {
        logger.info("startBlockchain() - start")
        stopBlockchain(chainId)

        withReadConnection(storage, chainId) { eContext ->
            val configuration = blockchainConfigProvider.getConfiguration(eContext, chainId)
            if (configuration != null) {
                val blockchainRID = DatabaseAccess.of(eContext).getBlockchainRID(eContext)!! // TODO: [et]: Fix Kotlin NPE
                val context = BaseBlockchainContext(blockchainRID, NODE_ID_AUTO, chainId, null)
                val blockchainConfig = blockchainInfrastructure.makeBlockchainConfiguration(configuration, context)

                val engine = blockchainInfrastructure.makeBlockchainEngine(blockchainConfig)
                blockchainProcesses[chainId] = blockchainInfrastructure.makeBlockchainProcess(engine) {
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
        blockchainProcesses.remove(chainId)
                ?.shutdown()
    }

    override fun shutdown() {
        executor.shutdownNow()
        executor.awaitTermination(1000, TimeUnit.MILLISECONDS)
        blockchainProcesses.forEach { _, process -> process.shutdown() }
        blockchainProcesses.clear()
        storage.close()
        blockchainInfrastructure.shutdown()
    }
}