// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain

import net.postchain.base.BaseApiInfrastructure
import net.postchain.base.BaseBlockchainInfrastructure
import net.postchain.base.BaseBlockchainProcessManager
import net.postchain.core.BlockchainInfrastructure
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

    fun stopBlockchain(chainID: Long) {
        processManager.stopBlockchain(chainID)
    }

    fun stopAllBlockchain() {
        processManager.shutdown()
    }
}