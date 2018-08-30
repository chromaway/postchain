// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain

import net.postchain.base.BaseBlockchainContext
import net.postchain.base.BaseBlockchainInfrastructure
import net.postchain.base.BaseConfigurationDataStore
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.base.withReadConnection
import net.postchain.core.*
import net.postchain.ebft.EBFTSynchronizationInfrastructure
import net.postchain.network.PeerConnectionManager
import org.apache.commons.configuration2.Configuration


class BaseBlockchainProcessManager(
        val blockchainInfrastructure: BlockchainInfrastructure,
        val synchronizationInfrastructure: SynchronizationInfrastructure,
        val nodeConfig: Configuration
) : BlockchainProcessManager {

    val storage = baseStorage(nodeConfig, NODE_ID_TODO)
    val dbAccess = SQLDatabaseAccess()

    val blockchainProcesses = mutableMapOf<Long, BlockchainProcess>()

    private fun runBlockchain(chainId: Long) {
        blockchainProcesses[chainId]?.shutdown()

        withReadConnection(storage, chainId) {
            ctx ->
            val blockchainRID = dbAccess.getBlockchainRID(ctx)!!
            val confData = BaseConfigurationDataStore.getConfigurationData(ctx, 0)
            val configuration = blockchainInfrastructure.makeBlockchainConfiguration(
                    confData, BaseBlockchainContext(blockchainRID, NODE_ID_AUTO, chainId, null)
            )
            val engine = blockchainInfrastructure.makeBlockchainEngine(configuration)
            val restartHandler = { runBlockchain(chainId) }
            blockchainProcesses[chainId] = synchronizationInfrastructure.makeBlockchainProcess(
                    engine, restartHandler
            )
            Unit
        }
    }

    override fun addBlockchain(chainID: Long) {
        runBlockchain(chainID)
    }

    override fun shutdown() {
        storage.close()
    }
}


/**
 * A postchain node
 *
 * @property connManager instance of [PeerConnectionManager]
 * @property blockchainInstance instance of [EBFTBlockchainInstance]
 */
class PostchainNode(nodeConfig: Configuration) {

    //lateinit var connManager: PeerConnectionManager<EbftMessage>
    val processManager: BaseBlockchainProcessManager
    val blockchainInfrastructure: BlockchainInfrastructure
    val syncInfrastructure: SynchronizationInfrastructure

    init {
        blockchainInfrastructure = BaseBlockchainInfrastructure(nodeConfig)
        syncInfrastructure = EBFTSynchronizationInfrastructure(nodeConfig)
        processManager = BaseBlockchainProcessManager(blockchainInfrastructure, syncInfrastructure, nodeConfig)
    }

    fun stop() {
        //connManager.stop() // TODO
        processManager.shutdown()
    }

    fun start(chainID: Long) {
        processManager.addBlockchain(chainID)
    }

    fun verifyConfiguration(ctx: EContext, nodeConfig: Configuration, blockchainRID: ByteArray) {
        val confData = BaseConfigurationDataStore.getConfigurationData(ctx, 0)
        val configuration = blockchainInfrastructure.makeBlockchainConfiguration(
                confData, BaseBlockchainContext(blockchainRID, ctx.nodeID, ctx.chainID, null))
    }
}