package net.postchain.test

import net.postchain.PostchainNode
import net.postchain.api.rest.controller.Model
import net.postchain.base.BaseApiInfrastructure
import net.postchain.base.BaseBlockchainConfigurationData
import net.postchain.base.BaseConfigurationDataStore
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.base.withWriteConnection
import net.postchain.baseStorage
import net.postchain.common.hexStringToByteArray
import net.postchain.core.BlockchainConfigurationFactory
import net.postchain.core.NODE_ID_TODO
import net.postchain.ebft.BlockchainInstanceModel
import net.postchain.gtx.encodeGTXValue
import org.apache.commons.configuration2.Configuration

class PostchainTestNode(nodeConfig: Configuration) : PostchainNode(nodeConfig) {

    init {
        val storage = baseStorage(nodeConfig, NODE_ID_TODO, true)
        val chainId = nodeConfig.getLong("activechainids")
        val blockchainRID = nodeConfig
                .getString("blockchain.$chainId.blockchainrid")
                .hexStringToByteArray()

        withWriteConnection(storage, chainId) { eContext ->
            with(SQLDatabaseAccess()) {
                initialize(eContext, expectedDbVersion = 1)
                checkBlockchainRID(eContext, blockchainRID)
            }
            true
        }

        val configData = BaseBlockchainConfigurationData.readFromCommonsConfiguration(
                nodeConfig, chainId, NODE_ID_TODO)
        val factoryClass = Class.forName(configData.data["configurationfactory"]!!.asString())
        val factory = (factoryClass.newInstance() as BlockchainConfigurationFactory)

        val blockchainConfiguration = factory.makeBlockchainConfiguration(configData, null)
        val configAsByteArray = encodeGTXValue(
                (blockchainConfiguration as BaseBlockchainConfiguration).configData.data)

        withWriteConnection(storage, chainId) { eContext ->
            BaseConfigurationDataStore.addConfigurationData(eContext, 0, configAsByteArray)
            true
        }
    }

    override fun isWipeDatabase(): Boolean = false

    fun getRestApiModel(chainId: Long): Model {
        val blockchainProcess = processManager.retrieveBlockchain(chainId)!!
        return (apiInfrastructure as BaseApiInfrastructure)
                .getApiModel(blockchainProcess)!!
    }

    fun getRestApiHttpPort(): Int {
        return (apiInfrastructure as BaseApiInfrastructure)
                .restApi?.actualPort() ?: 0
    }

    fun getBlockchainInstance(chainId: Long): BlockchainInstanceModel {
        return processManager.retrieveBlockchain(chainId) as BlockchainInstanceModel
    }

}