package net.postchain.base

import net.postchain.StorageBuilder
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.core.*
import org.apache.commons.configuration2.Configuration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BaseBlockchainProcessManager(
        private val blockchainInfrastructure: BlockchainInfrastructure,
        nodeConfig: Configuration
) : BlockchainProcessManager {

    val storage = StorageBuilder.buildStorage(nodeConfig, NODE_ID_TODO)
    private val dbAccess = SQLDatabaseAccess()
    private val blockchainProcesses = mutableMapOf<Long, BlockchainProcess>()
    private val executor = Executors.newSingleThreadExecutor()

    override fun startBlockchain(chainId: Long) {
        stopBlockchain(chainId)

        withReadConnection(storage, chainId) { eContext ->
            val lastHeight = dbAccess.getLastBlockHeight(eContext)
            val nextHeight = BaseConfigurationDataStore.findConfiguration(eContext, lastHeight + 1)
            if (nextHeight != null) {
                val blockchainRID = dbAccess.getBlockchainRID(eContext)!! // TODO: [et]: Fix Kotlin NPE
                val blockchainConfig = blockchainInfrastructure.makeBlockchainConfiguration(
                        BaseConfigurationDataStore.getConfigurationData(eContext, nextHeight)!!,
                        BaseBlockchainContext(blockchainRID, NODE_ID_AUTO, chainId, null))

                val engine = blockchainInfrastructure.makeBlockchainEngine(blockchainConfig)
                blockchainProcesses[chainId] = blockchainInfrastructure.makeBlockchainProcess(engine) {
                    executor.execute {
                        startBlockchain(chainId)
                    }
                }
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