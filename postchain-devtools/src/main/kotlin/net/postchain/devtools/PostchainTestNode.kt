package net.postchain.devtools

import mu.KLogging
import net.postchain.PostchainNode
import net.postchain.StorageBuilder
import net.postchain.api.rest.controller.Model
import net.postchain.base.*
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.base.data.DatabaseAccess
import net.postchain.common.toHex
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.*
import net.postchain.ebft.EBFTSynchronizationInfrastructure
import net.postchain.gtx.GTXValue
import net.postchain.gtx.encodeGTXValue

class PostchainTestNode(nodeConfigProvider: NodeConfigurationProvider, preWipeDatabase: Boolean) : PostchainNode(nodeConfigProvider) {

    private val storage: Storage
    val pubKey: String
    private var isInitialized = false

    init {
        val nodeConfig = nodeConfigProvider.getConfiguration()
        storage = StorageBuilder.buildStorage(nodeConfig, NODE_ID_TODO, preWipeDatabase)
        pubKey = nodeConfig.pubKey
    }

    companion object : KLogging() {
        const val DEFAULT_CHAIN_ID = 1L
    }

    private fun initDb(chainId: Long, blockchainRid: ByteArray) {
        // TODO: [et]: Is it necessary here after StorageBuilder.buildStorage() redesign?
        withWriteConnection(storage, chainId) { eContext ->
            with(DatabaseAccess.of(eContext)) {
                initialize(eContext.conn, expectedDbVersion = 1)
                checkBlockchainRID(eContext, blockchainRid)
            }
            true
        }

        isInitialized = true
    }

    fun addBlockchain(chainId: Long, blockchainRid: ByteArray, blockchainConfig: GTXValue) {
        initDb(chainId, blockchainRid)
        addConfiguration(chainId, 0, blockchainConfig)
    }


    fun addConfiguration(chainId: Long, height: Long, blockchainConfig: GTXValue) {
        check(isInitialized) { "PostchainNode is not initialized" }

        withWriteConnection(storage, chainId) { eContext ->
            BaseConfigurationDataStore.addConfigurationData(
                    eContext, height, encodeGTXValue(blockchainConfig))
            true
        }
    }

    fun startBlockchain() {
        startBlockchain(DEFAULT_CHAIN_ID)
    }

    override fun shutdown() {
        super.shutdown()
        storage.close()
    }

    fun getRestApiModel(): Model {
        val blockchainProcess = processManager.retrieveBlockchain(DEFAULT_CHAIN_ID)!!
        return ((blockchainInfrastructure as BaseBlockchainInfrastructure).apiInfrastructure as BaseApiInfrastructure)
                .restApi?.retrieveModel(blockchainRID(blockchainProcess))!!
    }

    fun getRestApiHttpPort(): Int {
        return ((blockchainInfrastructure as BaseBlockchainInfrastructure).apiInfrastructure as BaseApiInfrastructure)
                .restApi?.actualPort() ?: 0
    }

    fun getBlockchainInstance(chainId: Long = DEFAULT_CHAIN_ID): BlockchainProcess {
        return processManager.retrieveBlockchain(chainId) as BlockchainProcess
    }

    fun retrieveBlockchain(chainId: Long = DEFAULT_CHAIN_ID): BlockchainProcess? {
        return processManager.retrieveBlockchain(chainId)
    }

    fun transactionQueue(chainId: Long = DEFAULT_CHAIN_ID): TransactionQueue {
        return getBlockchainInstance(chainId).getEngine().getTransactionQueue()
    }

    fun blockQueries(chainId: Long = DEFAULT_CHAIN_ID): BlockQueries {
        return getBlockchainInstance(chainId).getEngine().getBlockQueries()
    }

    fun blockBuildingStrategy(chainId: Long = DEFAULT_CHAIN_ID): BlockBuildingStrategy {
        return getBlockchainInstance(chainId).getEngine().getBlockBuildingStrategy()
    }

    fun networkTopology(chainId: Long = DEFAULT_CHAIN_ID): Map<String, String> {
        // TODO: [et]: Fix type casting
        return ((blockchainInfrastructure as BaseBlockchainInfrastructure)
                .synchronizationInfrastructure as EBFTSynchronizationInfrastructure)
                .connectionManager.getPeersTopology(chainId)
                .mapKeys { pubKeyToConnection ->
                    pubKeyToConnection.key.toString()
                }
    }

    private fun blockchainRID(process: BlockchainProcess): String {
        return (process.getEngine().getConfiguration() as BaseBlockchainConfiguration) // TODO: [et]: Resolve type cast
                .blockchainRID.toHex()
    }
}