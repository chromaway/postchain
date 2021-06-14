// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain

import mu.KLogging
import net.postchain.base.BlockchainRid
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.BaseInfrastructureFactoryProvider
import net.postchain.core.BlockchainInfrastructure
import net.postchain.core.BlockchainProcessManager
import net.postchain.core.Shutdownable
import net.postchain.debug.BlockTrace
import net.postchain.debug.BlockchainProcessName
import net.postchain.debug.DefaultNodeDiagnosticContext
import net.postchain.debug.DiagnosticProperty
import net.postchain.devtools.PeerNameHelper.peerName

/**
 * Postchain node instantiates infrastructure and blockchain process manager.
 */
open class PostchainNode(val nodeConfigProvider: NodeConfigurationProvider) : Shutdownable {

    protected val blockchainInfrastructure: BlockchainInfrastructure
    val processManager: BlockchainProcessManager
    private val diagnosticContext = DefaultNodeDiagnosticContext()

    companion object : KLogging()

    init {
        val infrastructureFactory = BaseInfrastructureFactoryProvider().createInfrastructureFactory(nodeConfigProvider)
        blockchainInfrastructure = infrastructureFactory.makeBlockchainInfrastructure(nodeConfigProvider, diagnosticContext)
        val blockchainConfigProvider = infrastructureFactory.makeBlockchainConfigurationProvider()
        processManager = infrastructureFactory.makeProcessManager(
                nodeConfigProvider, blockchainInfrastructure, blockchainConfigProvider, diagnosticContext)

        diagnosticContext.addProperty(DiagnosticProperty.VERSION, getVersion())
        diagnosticContext.addProperty(DiagnosticProperty.PUB_KEY, nodeConfigProvider.getConfiguration().pubKey)
    }

    fun startBlockchain(chainId: Long): BlockchainRid? {
        return processManager.startBlockchain(chainId, buildBbDebug(chainId))
    }

    fun stopBlockchain(chainId: Long) {
        processManager.stopBlockchain(chainId, buildBbDebug(chainId))
    }

    override fun shutdown() {
        // FYI: Order is important
        logger.debug("${name()}: Stopping ProcessManager")
        processManager.shutdown()
        logger.debug("${name()}: Stopping BlockchainInfrastructure")
        blockchainInfrastructure.shutdown()
        logger.debug("${name()}: Closing NodeConfigurationProvider")
        nodeConfigProvider.close()
        logger.debug("${name()}: Stopped PostchainNode")
    }

    private fun name(): String {
        return peerName(diagnosticContext.getProperty(DiagnosticProperty.PUB_KEY).toString())
    }

    /**
     * This is for DEBUG operation only
     *
     * @return "true" if we are actually running a test. If we are inside a test we can ofter do more
     * debugging than otherwise
     */
    open fun isThisATest(): Boolean = false

    /**
     * This is for DEBUG operation only
     *
     * We don't care about what the most recent block was, or height at this point.
     * We are just providing the info we have right now
     */
    private fun buildBbDebug(chainId: Long): BlockTrace? {
        return if (logger.isDebugEnabled) {
            val x = processManager.retrieveBlockchain(chainId)
            if (x == null) {
                logger.trace("WARN why didn't we find the blockchain for chainId: $chainId on node: ${nodeConfigProvider.getConfiguration().pubKey}?")
                null
            } else {
                val procName = BlockchainProcessName(nodeConfigProvider.getConfiguration().pubKey, x.getEngine().getConfiguration().blockchainRid)
                BlockTrace.buildBeforeBlock(procName)
            }
        } else {
            null
        }
    }

    private fun getVersion(): String {
        return javaClass.getPackage()?.implementationVersion ?: "null"
    }
}
