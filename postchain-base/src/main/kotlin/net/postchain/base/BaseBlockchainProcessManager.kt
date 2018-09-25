package net.postchain.base

import net.postchain.StorageBuilder
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.core.*
import org.apache.commons.configuration2.Configuration

class BaseBlockchainProcessManager(
        private val blockchainInfrastructure: BlockchainInfrastructure,
        nodeConfig: Configuration
) : BlockchainProcessManager {

    val storage = StorageBuilder.buildStorage(nodeConfig, NODE_ID_TODO)
    private val dbAccess = SQLDatabaseAccess()
    private val blockchainProcesses = mutableMapOf<Long, BlockchainProcess>()

    override fun startBlockchain(chainId: Long) {
        blockchainProcesses[chainId]?.shutdown()

        withReadConnection(storage, chainId) { eContext ->
            val blockchainRID = dbAccess.getBlockchainRID(eContext)!! // TODO: [et]: Fix Kotlin NPE
            val blockchainConfig = blockchainInfrastructure.makeBlockchainConfiguration(
                    BaseConfigurationDataStore.getConfigurationData(eContext, 0),
                    BaseBlockchainContext(blockchainRID, NODE_ID_AUTO, chainId, null))

            val engine = blockchainInfrastructure.makeBlockchainEngine(blockchainConfig)
            blockchainProcesses[chainId] = blockchainInfrastructure.makeBlockchainProcess(engine) {
                startBlockchain(chainId)
            }

            Unit
        }
    }

    override fun retrieveBlockchain(chainId: Long): BlockchainProcess? {
        return blockchainProcesses[chainId]
    }

    override fun shutdown() {
        blockchainProcesses.forEach { _, process -> process.shutdown() }
        storage.close()
        blockchainInfrastructure.shutdown()
    }
}