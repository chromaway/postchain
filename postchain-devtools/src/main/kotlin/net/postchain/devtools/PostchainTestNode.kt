// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools

import mu.KLogging
import net.postchain.PostchainNode
import net.postchain.StorageBuilder
import net.postchain.api.rest.controller.Model
import net.postchain.base.*
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.base.data.DatabaseAccess
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.*
import net.postchain.devtools.utils.configuration.BlockchainSetup
import net.postchain.ebft.EBFTSynchronizationInfrastructure
import net.postchain.gtv.Gtv
import kotlin.properties.Delegates

/**
 * This node is used in integration tests.
 *
 * @property nodeConfigProvider gives us the configuration of the node
 * @property preWipeDatabase is true if we want to start up clean (usually the case when we run tests)
 *
 */
class PostchainTestNode(
        nodeConfigProvider: NodeConfigurationProvider,
        preWipeDatabase: Boolean = false
) : PostchainNode(nodeConfigProvider) {

    private val testStorage: Storage
    val pubKey: String
    private var isInitialized by Delegates.notNull<Boolean>()
    private val blockchainRidMap = mutableMapOf<Long, BlockchainRid>() // Used to keep track of the BC RIDs of the chains

    init {
        val nodeConfig = nodeConfigProvider.getConfiguration()
        testStorage = StorageBuilder.buildStorage(nodeConfig.appConfig, NODE_ID_TODO, preWipeDatabase)
        pubKey = nodeConfig.pubKey
        isInitialized = true
    }

    companion object : KLogging() {
        const val SYSTEM_CHAIN_IID = 0L
        const val DEFAULT_CHAIN_IID = 1L
    }

    fun addBlockchain(chainSetup: BlockchainSetup) {
        addBlockchain(chainSetup.chainId.toLong(), chainSetup.bcGtv)
    }

    fun addBlockchain(chainId: Long, blockchainConfig: Gtv): BlockchainRid {
        check(isInitialized) { "PostchainNode is not initialized" }

        return withReadWriteConnection(testStorage, chainId) { eContext: EContext ->
            val brid = BlockchainRidFactory.calculateBlockchainRid(blockchainConfig)
            logger.debug("Adding blockchain: chainId: $chainId, blockchainRid: ${brid.toHex()}")
            DatabaseAccess.of(eContext).initializeBlockchain(eContext, brid)
            BaseConfigurationDataStore.addConfigurationData(eContext, 0, blockchainConfig)
            brid
        }
    }

    fun addConfiguration(chainId: Long, height: Long, blockchainConfig: Gtv): BlockchainRid {
        check(isInitialized) { "PostchainNode is not initialized" }

        return withReadWriteConnection(testStorage, chainId) { eContext: EContext ->
            logger.debug("Adding configuration for chain: $chainId, height: $height")
            val brid = BlockchainRidFactory.calculateBlockchainRid(blockchainConfig)
            BaseConfigurationDataStore.addConfigurationData(eContext, height, blockchainConfig)
            brid
        }
    }

    fun startBlockchain(): BlockchainRid? {
        return startBlockchain(DEFAULT_CHAIN_IID)
    }

    override fun shutdown() {
        super.shutdown()
        testStorage.close()
    }

    fun getRestApiModel(): Model {
        val blockchainProcess = processManager.retrieveBlockchain(DEFAULT_CHAIN_IID)!!
        return ((blockchainInfrastructure as BaseBlockchainInfrastructure).apiInfrastructure as BaseApiInfrastructure)
                .restApi?.retrieveModel(blockchainRID(blockchainProcess))!!
    }

    fun getRestApiHttpPort(): Int {
        return ((blockchainInfrastructure as BaseBlockchainInfrastructure).apiInfrastructure as BaseApiInfrastructure)
                .restApi?.actualPort() ?: 0
    }

    fun getBlockchainInstance(chainId: Long = DEFAULT_CHAIN_IID): BlockchainProcess {
        return processManager.retrieveBlockchain(chainId) as BlockchainProcess
    }

    fun retrieveBlockchain(chainId: Long = DEFAULT_CHAIN_IID): BlockchainProcess? {
        return processManager.retrieveBlockchain(chainId)
    }

    fun transactionQueue(chainId: Long = DEFAULT_CHAIN_IID): TransactionQueue {
        return getBlockchainInstance(chainId).getEngine().getTransactionQueue()
    }

    fun blockQueries(chainId: Long = DEFAULT_CHAIN_IID): BlockQueries {
        return getBlockchainInstance(chainId).getEngine().getBlockQueries()
    }

    fun blockBuildingStrategy(chainId: Long = DEFAULT_CHAIN_IID): BlockBuildingStrategy {
        return getBlockchainInstance(chainId).getEngine().getBlockBuildingStrategy()
    }

    fun networkTopology(chainId: Long = DEFAULT_CHAIN_IID): Map<String, String> {
        // TODO: [et]: Fix type casting
        return ((blockchainInfrastructure as BaseBlockchainInfrastructure)
                .synchronizationInfrastructure as EBFTSynchronizationInfrastructure)
                .connectionManager.getPeersTopology(chainId)
                .mapKeys { pubKeyToConnection ->
                    pubKeyToConnection.key.toString()
                }
    }

    fun mapBlockchainRID(chainId: Long, bcRID: BlockchainRid) {
        blockchainRidMap[chainId] = bcRID
    }

    /**
     * Yeah I know this is a strange way of retrieving the BC RID, but plz change if you think of something better.
     * (It's only for test, so I didn't ptu much thought into it. )
     */
    fun getBlockchainRid(chainId: Long): BlockchainRid? = blockchainRidMap[chainId]

    private fun blockchainRID(process: BlockchainProcess): String {
        return (process.getEngine().getConfiguration() as BaseBlockchainConfiguration) // TODO: [et]: Resolve type cast
                .blockchainRid.toHex()
    }
}