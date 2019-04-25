package net.postchain.base

import net.postchain.api.rest.controller.PostchainModel
import net.postchain.api.rest.controller.RestApi
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.common.toHex
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.ApiInfrastructure
import net.postchain.core.BlockchainProcess
import net.postchain.ebft.worker.WorkerBase

class BaseApiInfrastructure(nodeConfigProvider: NodeConfigurationProvider) : ApiInfrastructure {

    val restApi: RestApi? = with(nodeConfigProvider.getConfiguration()) {
        if (restApiPort != -1) {
            if (restApiSsl) RestApi(restApiPort, restApiBasePath, restApiSslCertificate, restApiSslCertificatePassword)
            else RestApi(restApiPort, restApiBasePath)
        } else {
            null
        }
    }

    override fun connectProcess(process: BlockchainProcess) {
        restApi?.run {
            val engine = process.getEngine()

            val apiModel = PostchainModel(
                    (process as WorkerBase).networkAwareTxQueue,
                    engine.getConfiguration().getTransactionFactory(),
                    engine.getBlockQueries() as BaseBlockQueries) // TODO: [et]: Resolve type cast

            attachModel(blockchainRID(process), apiModel)
        }
    }

    override fun disconnectProcess(process: BlockchainProcess) {
        restApi?.detachModel(blockchainRID(process))
    }

    override fun shutdown() {
        restApi?.stop()
    }

    private fun blockchainRID(process: BlockchainProcess): String {
        return (process.getEngine().getConfiguration() as BaseBlockchainConfiguration) // TODO: [et]: Resolve type cast
                .blockchainRID.toHex()
    }
}