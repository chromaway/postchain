package net.postchain.base

import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.controller.PostchainModel
import net.postchain.api.rest.controller.RestApi
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.common.toHex
import net.postchain.core.ApiInfrastructure
import net.postchain.core.BlockchainProcess
import net.postchain.core.NODE_ID_AUTO
import net.postchain.ebft.CommManager
import net.postchain.ebft.message.EbftMessage
import org.apache.commons.configuration2.Configuration

class BaseApiInfrastructure(val config: Configuration) : ApiInfrastructure {

    val restApi: RestApi?

    init {
        val port = config.getInt("api.port", 7740)
        val basePath = config.getString("api.basepath", "")
        restApi = if (port != -1) RestApi(port, basePath) else null
    }

    override fun connectProcess(process: BlockchainProcess, communicationManager: CommManager<EbftMessage>) {
        restApi?.run {
            val engine = process.getEngine()

            val apiTxQueue = NetworkAwareTxQueue(
                    engine.getTransactionQueue(),
                    communicationManager,
                    NODE_ID_AUTO)

            val apiModel = PostchainModel(
                    apiTxQueue,
                    engine.getConfiguration().getTransactionFactory(),
                    engine.getBlockQueries() as BaseBlockQueries) // TODO: [et]: Resolve type cast

            attachModel(blockchainRID(process), apiModel)
        }
    }

    override fun disconnectProcess(process: BlockchainProcess) {
        restApi?.detachModel(blockchainRID(process))
    }

    override fun shutdown() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getApiModel(process: BlockchainProcess): Model? =
            restApi?.retrieveModel(blockchainRID(process))

    private fun blockchainRID(process: BlockchainProcess): String {
        return (process.getEngine().getConfiguration() as BaseBlockchainConfiguration) // TODO: [et]: Resolve type cast
                .blockchainRID.toHex()
    }
}