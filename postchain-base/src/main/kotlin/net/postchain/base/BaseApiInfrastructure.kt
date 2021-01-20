// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

import net.postchain.api.rest.controller.DefaultDebugInfoQuery
import net.postchain.api.rest.controller.RestApi
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.base.data.BaseTransactionQueue
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.ApiInfrastructure
import net.postchain.core.BlockchainProcess
import net.postchain.core.NodeStateTracker
import net.postchain.debug.NodeDiagnosticContext
import net.postchain.ebft.rest.model.PostchainEBFTModel
import net.postchain.ebft.worker.ValidatorWorker

class BaseApiInfrastructure(
        nodeConfigProvider: NodeConfigurationProvider,
        val nodeDiagnosticContext: NodeDiagnosticContext
) : ApiInfrastructure {

    val restApi: RestApi? = with(nodeConfigProvider.getConfiguration()) {
        if (restApiPort != -1) {
            if (restApiSsl) {
                RestApi(
                        restApiPort,
                        restApiBasePath,
                        restApiSslCertificate,
                        restApiSslCertificatePassword)
            } else {
                RestApi(
                        restApiPort,
                        restApiBasePath)
            }
        } else {
            null
        }
    }

    override fun connectProcess(process: BlockchainProcess) {
        if (restApi != null) {
            val engine = process.getEngine()

            val apiModel = PostchainEBFTModel(
                    process.getEngine().getConfiguration().chainID,
                    if (process is ValidatorWorker) process.nodeStateTracker else NodeStateTracker(),
                    if (process is ValidatorWorker) process.networkAwareTxQueue else process.getEngine().getTransactionQueue(),
                    engine.getConfiguration().getTransactionFactory(),
                    engine.getBlockQueries() as BaseBlockQueries, // TODO: [et]: Resolve type cast
                    DefaultDebugInfoQuery(nodeDiagnosticContext)
            )

            restApi.attachModel(blockchainRID(process), apiModel)
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
                .blockchainRid.toHex()
    }
}