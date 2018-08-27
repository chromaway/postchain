// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain

import net.postchain.base.BaseBlockchainContext
import net.postchain.base.BaseBlockchainInfrastructure
import net.postchain.base.BaseConfigurationDataStore
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.base.withReadConnection
import net.postchain.common.hexStringToByteArray
import net.postchain.config.CommonsConfigurationFactory
import net.postchain.core.*
import net.postchain.ebft.BlockchainInstanceModel
import net.postchain.ebft.EBFTBlockchainInstanceWorker
import net.postchain.ebft.EBFTSynchronizationInfrastructure
import net.postchain.network.PeerConnectionManager
import org.apache.commons.configuration2.Configuration

class BaseBlockchainProcessManager(
        val blockchainInfrastructure: BlockchainInfrastructure,
        val synchronizationInfrastructure: SynchronizationInfrastructure,
        val nodeConfig: Configuration
) : BlockchainProcessManager {

    val storage = baseStorage(nodeConfig, -1 ) // TODO: remove nodeIndex
    val dbAccess = SQLDatabaseAccess()

    val blockchainProcesses = mutableMapOf<Long, BlockchainProcess>()

    private fun runBlockchain(chainId: Long) {
        blockchainProcesses[chainId]?.shutdown()

        withReadConnection(storage, chainId) {
            ctx ->
            val blockchainRID = dbAccess.getBlockchainRID(ctx)!!
            val confData = BaseConfigurationDataStore.getConfigurationData(ctx, 0)
            val configuration = blockchainInfrastructure.makeBlockchainConfiguration(
                    confData, BaseBlockchainContext(blockchainRID, -1, chainId)
            )
            val engine = blockchainInfrastructure.makeBlockchainEngine(configuration)
            blockchainProcesses[chainId] = synchronizationInfrastructure.makeBlockchainProcess(engine) as EBFTBlockchainInstanceWorker
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
class PostchainNode {

    //lateinit var connManager: PeerConnectionManager<EbftMessage>
    lateinit var blockchainProcess: EBFTBlockchainInstanceWorker

    fun stop() {
        //connManager.stop()
        blockchainProcess.shutdown()
    }

    fun getModel(): BlockchainInstanceModel {
        return blockchainProcess
    }

    /**
     * Start the postchain node by setting up everything and finally starting the updateLoop thread
     *
     * @param config configuration settings for the node
     * @param nodeIndex the index of the node
     */
    fun start(nodeConfig: Configuration, nodeIndex: Int) {
        // This will eventually become a list of chain ids.
        // But for now it's just a single integer.
        val chainId = nodeConfig.getLong("activechainids")
        val blockchainRID = nodeConfig.getString("blockchain.${chainId}.blockchainrid").hexStringToByteArray() // TODO

        val storage = baseStorage(nodeConfig, -1 /*Will be eliminate later*/)

        val confData = withReadConnection(storage, chainId) {
            BaseConfigurationDataStore.getConfigurationData(it, 0)
        }

        val blockchainInfrastructure = BaseBlockchainInfrastructure(nodeConfig)
        val syncInfrastructure = EBFTSynchronizationInfrastructure(nodeConfig)
        val configuration = blockchainInfrastructure.makeBlockchainConfiguration(
                confData, BaseBlockchainContext(blockchainRID, nodeIndex, chainId))
        val engine = blockchainInfrastructure.makeBlockchainEngine(configuration)
        blockchainProcess = syncInfrastructure.makeBlockchainProcess(engine) as EBFTBlockchainInstanceWorker
    }

    fun verifyConfiguration(ctx: EContext, nodeConfig: Configuration, blockchainRID: ByteArray) {
        val confData = BaseConfigurationDataStore.getConfigurationData(ctx, 0)

        val blockchainInfrastructure = BaseBlockchainInfrastructure(nodeConfig)
        val syncInfrastructure = EBFTSynchronizationInfrastructure(nodeConfig)
        val configuration = blockchainInfrastructure.makeBlockchainConfiguration(
                confData, BaseBlockchainContext(blockchainRID, ctx.nodeID, ctx.chainID))
    }

    /**
     * Pre-start function used to process the configuration file before calling the final [start] function
     *
     * @param configFile configuration file to parse
     * @param nodeIndex index of the local node
     */
    fun start(configFile: String, nodeIndex: Int) {
        start(CommonsConfigurationFactory.readFromFile(configFile), nodeIndex)
    }

}