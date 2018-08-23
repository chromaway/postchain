// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain

import net.postchain.base.BaseBlockchainContext
import net.postchain.base.BaseBlockchainInfrastructure
import net.postchain.common.hexStringToByteArray
import net.postchain.config.CommonsConfigurationFactory
import net.postchain.ebft.BlockchainInstanceModel
import net.postchain.ebft.EBFTBlockchainInstanceWorker
import net.postchain.ebft.EBFTSynchronizationInfrastructure
import net.postchain.network.PeerConnectionManager
import org.apache.commons.configuration2.Configuration

/**
 * A postchain node
 *
 * @property connManager instance of [PeerConnectionManager]
 * @property blockchainInstance instance of [EBFTBlockchainInstance]
 */
class PostchainNode2 {

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
    fun start(config: Configuration, nodeIndex: Int) {
        // This will eventually become a list of chain ids.
        // But for now it's just a single integer.
        val chainId = config.getLong("activechainids")
        val blockchainRID = config.getString("blockchain.${chainId}.blockchainrid").hexStringToByteArray() // TODO

        val blockchainInfrastructure = BaseBlockchainInfrastructure(config)
        val syncInfrastructure = EBFTSynchronizationInfrastructure(config)
        val configuration = blockchainInfrastructure.makeBlockchainConfiguration(
                ByteArray(0), BaseBlockchainContext(blockchainRID, 42, chainId)) // TODO
        val engine = blockchainInfrastructure.makeBlockchainEngine(configuration)
        blockchainProcess = syncInfrastructure.makeBlockchainProcess(engine) as EBFTBlockchainInstanceWorker
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