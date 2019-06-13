package net.postchain.base

import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.*

class TestBlockchainProcess(val _engine: BlockchainEngine) : BlockchainProcess {
    override fun getEngine(): BlockchainEngine {
        return _engine
    }

    override fun shutdown() {
        _engine.shutdown()
    }
}


class TestSynchronizationInfrastructure : SynchronizationInfrastructure {
    override fun makeBlockchainProcess(processName: String, engine: BlockchainEngine, restartHandler: RestartHandler): BlockchainProcess {
        return TestBlockchainProcess(engine)
    }

    override fun shutdown() {}
}

class BaseTestInfrastructureFactory : InfrastructureFactory {
    override fun makeBlockchainInfrastructure(nodeConfigProvider: NodeConfigurationProvider): BlockchainInfrastructure {
        return BaseBlockchainInfrastructure(
                nodeConfigProvider,
                TestSynchronizationInfrastructure(),
                BaseApiInfrastructure(nodeConfigProvider))
    }

    override fun makeProcessManager(
            nodeConfigProvider: NodeConfigurationProvider,
            blockchainConfig: BlockchainConfigurationProvider,
            blockchainInfrastructure: BlockchainInfrastructure
    ): BlockchainProcessManager {
        return BaseBlockchainProcessManager(blockchainInfrastructure, nodeConfigProvider, blockchainConfig)
    }
}
