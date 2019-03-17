package net.postchain.base

import net.postchain.StorageBuilder
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.config.CommonsConfigurationFactory
import net.postchain.core.*
import org.apache.commons.configuration2.Configuration

class BaseBlockchainProcessManager(
        private val blockchainInfrastructure: BlockchainInfrastructure,
        nodeConfig: Configuration
) : BlockchainProcessManager {

    val storage = StorageBuilder.buildStorage(nodeConfig, NODE_ID_TODO)
    private val sqlCommands = CommonsConfigurationFactory.getSQLCommandsImplementation(nodeConfig.getString("database.driverclass"))
    private val dbAccess = SQLDatabaseAccess(sqlCommands)
    private val blockchainProcesses = mutableMapOf<Long, BlockchainProcess>()

    override fun startBlockchain(chainId: Long) {
        stopBlockchain(chainId)

        withReadConnection(storage, chainId) { eContext ->
            val blockchainRID = dbAccess.getBlockchainRID(eContext)!! // TODO: [et]: Fix Kotlin NPE

            // TODO: [et]: Starting with 0-height config
            val configurationData = BaseConfigurationDataStore.getConfigurationData(eContext, 0)
            if (configurationData != null) {
                val blockchainConfig = blockchainInfrastructure.makeBlockchainConfiguration(
                        configurationData,
                        BaseBlockchainContext(blockchainRID, NODE_ID_AUTO, chainId, null))

                val engine = blockchainInfrastructure.makeBlockchainEngine(blockchainConfig)
                blockchainProcesses[chainId] = blockchainInfrastructure.makeBlockchainProcess(engine) {
                    startBlockchain(chainId)
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
        blockchainProcesses.forEach { _, process -> process.shutdown() }
        storage.close()
        blockchainInfrastructure.shutdown()
    }
}