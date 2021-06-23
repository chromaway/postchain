// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

import mu.KLogging
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.blockchain.ManualBlockchainConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.*
import net.postchain.debug.BlockchainProcessName
import net.postchain.debug.NodeDiagnosticContext

class TestBlockchainProcess(val _engine: BlockchainEngine) : BlockchainProcess {

    companion object : KLogging()

    // Need this stuff to make this test class look a bit "normal"
    val processName: BlockchainProcessName = BlockchainProcessName("?", _engine.getConfiguration().blockchainRid)

    override fun getEngine(): BlockchainEngine {
        return _engine
    }

    override fun shutdown() {
        shutdownDebug("Begin")
        _engine.shutdown()
        shutdownDebug("End")
    }

    private fun shutdownDebug(str: String) {
        if (logger.isDebugEnabled) {
            logger.debug("$processName: shutdown() - $str.")
        }
    }
}


class TestSynchronizationInfrastructure : SynchronizationInfrastructure {

    override fun makeBlockchainProcess(processName: BlockchainProcessName, engine: BlockchainEngine, historicBlockchainContext: HistoricBlockchainContext?): BlockchainProcess {
        return TestBlockchainProcess(engine)
    }

    override fun exitBlockchainProcess(process: BlockchainProcess) = Unit
    override fun restartBlockchainProcess(process: BlockchainProcess) = Unit


    override fun shutdown() = Unit
}

class BaseTestInfrastructureFactory : InfrastructureFactory {

    override fun makeBlockchainConfigurationProvider(): BlockchainConfigurationProvider {
        return ManualBlockchainConfigurationProvider()
    }

    override fun makeBlockchainInfrastructure(
            nodeConfigProvider: NodeConfigurationProvider,
            nodeDiagnosticContext: NodeDiagnosticContext
    ): BlockchainInfrastructure {

        val syncInfra = TestSynchronizationInfrastructure()
        val apiInfra = BaseApiInfrastructure(nodeConfigProvider, nodeDiagnosticContext)

        return BaseBlockchainInfrastructure(
                nodeConfigProvider, syncInfra, apiInfra, nodeDiagnosticContext)
    }

    override fun makeProcessManager(
            nodeConfigProvider: NodeConfigurationProvider,
            blockchainInfrastructure: BlockchainInfrastructure,
            blockchainConfigurationProvider: BlockchainConfigurationProvider,
            nodeDiagnosticContext: NodeDiagnosticContext
    ): BlockchainProcessManager {

        return BaseBlockchainProcessManager(
                blockchainInfrastructure,
                nodeConfigProvider,
                blockchainConfigurationProvider,
                nodeDiagnosticContext)
    }
}
