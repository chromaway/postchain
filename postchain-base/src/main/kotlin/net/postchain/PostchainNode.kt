// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain

import net.postchain.base.*
import net.postchain.core.BlockchainInfrastructure
import net.postchain.core.EContext
import net.postchain.ebft.EBFTSynchronizationInfrastructure
import org.apache.commons.configuration2.Configuration

open class PostchainNode(nodeConfig: Configuration) {

    protected val processManager: BaseBlockchainProcessManager
    protected val blockchainInfrastructure: BlockchainInfrastructure

    init {
        blockchainInfrastructure = BaseBlockchainInfrastructure(
                nodeConfig,
                EBFTSynchronizationInfrastructure(nodeConfig),
                BaseApiInfrastructure(nodeConfig))

        processManager = BaseBlockchainProcessManager(
                blockchainInfrastructure,
                nodeConfig)
    }

    fun startBlockchain(chainID: Long) {
        processManager.startBlockchain(chainID)
    }

    fun stopAllBlockchain() {
        processManager.shutdown()
    }

    fun verifyConfiguration(ctx: EContext, nodeConfig: Configuration, blockchainRID: ByteArray) {
        val confData = BaseConfigurationDataStore.getConfigurationData(ctx, 0)
        val configuration = blockchainInfrastructure.makeBlockchainConfiguration(
                confData, BaseBlockchainContext(blockchainRID, ctx.nodeID, ctx.chainID, null))
    }
}