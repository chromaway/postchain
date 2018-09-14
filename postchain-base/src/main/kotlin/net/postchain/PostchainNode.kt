// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain

import net.postchain.base.*
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.core.*
import net.postchain.ebft.EBFTSynchronizationInfrastructure
import org.apache.commons.configuration2.Configuration

class BaseBlockchainProcessManager(
        private val blockchainInfrastructure: BlockchainInfrastructure,
        private val synchronizationInfrastructure: SynchronizationInfrastructure,
        private val apiInfrastructure: ApiInfrastructure,
        private val networkInfrastructure: NetworkInfrastructure,
        nodeConfig: Configuration,
        private val wipeDatabase: Boolean
) : BlockchainProcessManager {

    val storage = baseStorage(nodeConfig, NODE_ID_TODO, null)
    private val dbAccess = SQLDatabaseAccess()
    private val blockchainProcesses = mutableMapOf<Long, BlockchainProcess>()

    override fun startBlockchain(chainId: Long) {
        blockchainProcesses[chainId]?.shutdown()

        checkDbInitialized(chainId)
        withReadConnection(storage, chainId) { eContext ->
            val blockchainRID = dbAccess.getBlockchainRID(eContext)!! // TODO: [et]: Fix Kotlin NPE
            val blockchainConfig = blockchainInfrastructure.makeBlockchainConfiguration(
                    BaseConfigurationDataStore.getConfigurationData(eContext, 0),
                    BaseBlockchainContext(blockchainRID, NODE_ID_AUTO, chainId, null))

            val engine = blockchainInfrastructure.makeBlockchainEngine(blockchainConfig, wipeDatabase)
            val communicationManager = networkInfrastructure.buildCommunicationManager(blockchainConfig)
            blockchainProcesses[chainId] = synchronizationInfrastructure.makeBlockchainProcess(
                    engine, communicationManager) { startBlockchain(chainId) }

            apiInfrastructure.connectProcess(
                    blockchainProcesses[chainId]!!, communicationManager)

            Unit
        }
    }

    override fun retrieveBlockchain(chainId: Long): BlockchainProcess? {
        return blockchainProcesses[chainId]
    }

    override fun shutdown() {
        storage.close()
        blockchainProcesses.forEach { _, process -> process.shutdown() }
    }

    private fun checkDbInitialized(chainId: Long) {
        withWriteConnection(storage, chainId) { eContext ->
            dbAccess.initialize(eContext, expectedDbVersion = 1)
            true
        }
    }
}

open class PostchainNode(nodeConfig: Configuration) {

    //lateinit var connManager: PeerConnectionManager<EbftMessage>
    protected val processManager: BaseBlockchainProcessManager
    protected val blockchainInfrastructure: BlockchainInfrastructure
    protected val syncInfrastructure: SynchronizationInfrastructure
    protected val apiInfrastructure: ApiInfrastructure
    protected val networkInfrastructure: NetworkInfrastructure

    init {
        blockchainInfrastructure = BaseBlockchainInfrastructure(nodeConfig)
        syncInfrastructure = EBFTSynchronizationInfrastructure(nodeConfig)
        apiInfrastructure = BaseApiInfrastructure(nodeConfig)
        networkInfrastructure = BaseNetworkInfrastructure(nodeConfig)

        processManager = BaseBlockchainProcessManager(
                blockchainInfrastructure,
                syncInfrastructure,
                apiInfrastructure,
                networkInfrastructure,
                nodeConfig,
                isWipeDatabase())
    }

    protected open fun isWipeDatabase(): Boolean = true

    fun startBlockchain(chainID: Long) {
        processManager.startBlockchain(chainID)
    }

    fun stopAllBlockchain() {
        //connManager.stop() // TODO
        processManager.shutdown()
    }

    fun verifyConfiguration(ctx: EContext, nodeConfig: Configuration, blockchainRID: ByteArray) {
        val confData = BaseConfigurationDataStore.getConfigurationData(ctx, 0)
        val configuration = blockchainInfrastructure.makeBlockchainConfiguration(
                confData, BaseBlockchainContext(blockchainRID, ctx.nodeID, ctx.chainID, null))
    }
}