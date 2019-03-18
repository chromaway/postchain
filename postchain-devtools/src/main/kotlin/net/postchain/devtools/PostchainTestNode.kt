package net.postchain.devtools

import mu.KLogging
import net.postchain.PostchainNode
import net.postchain.StorageBuilder
import net.postchain.api.rest.controller.Model
import net.postchain.base.BaseApiInfrastructure
import net.postchain.base.BaseBlockchainInfrastructure
import net.postchain.base.BaseConfigurationDataStore
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.base.data.DatabaseAccess
import net.postchain.base.withWriteConnection
import net.postchain.common.toHex
import net.postchain.core.*
import net.postchain.gtx.GTXValue
import net.postchain.gtx.encodeGTXValue
import org.apache.commons.configuration2.Configuration

class PostchainTestNode(nodeConfig: Configuration) : PostchainNode(nodeConfig) {

    private val storage = StorageBuilder.buildStorage(nodeConfig, NODE_ID_TODO, true)

    companion object : KLogging() {
        const val DEFAULT_CHAIN_ID = 1L
    }

    fun addBlockchain(chainId: Long, blockchainRid: ByteArray, blockchainConfig: GTXValue) {
        // TODO: [et]: Is it necessary here after StorageBuilder.buildStorage() redesign?
        withWriteConnection(storage, chainId) { eContext ->
            with(DatabaseAccess.of(eContext)) {
                initialize(eContext.conn, expectedDbVersion = 1)
                checkBlockchainRID(eContext, blockchainRid)
            }
            true
        }

        withWriteConnection(storage, chainId) { eContext ->
            BaseConfigurationDataStore.addConfigurationData(
                    eContext, 0, encodeGTXValue(blockchainConfig))
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

    private fun blockchainRID(process: BlockchainProcess): String {
        return (process.getEngine().getConfiguration() as BaseBlockchainConfiguration) // TODO: [et]: Resolve type cast
                .blockchainRID.toHex()
    }
}