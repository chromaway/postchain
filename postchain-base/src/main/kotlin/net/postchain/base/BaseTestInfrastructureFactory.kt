package net.postchain.base

import net.postchain.core.*
import org.apache.commons.configuration2.Configuration

class TestBlockchainProcess(val _engine: BlockchainEngine): BlockchainProcess {
    override fun getEngine(): BlockchainEngine {
        return _engine
    }

    override fun shutdown() {
        _engine.shutdown()
    }
}


class TestSynchronizationInfrastructure: SynchronizationInfrastructure {
    override fun makeBlockchainProcess(engine: BlockchainEngine, restartHandler: RestartHandler): BlockchainProcess {
        return TestBlockchainProcess(engine)
    }

    override fun shutdown() {}
}

class BaseTestInfrastructureFactory: InfrastructureFactory {
    override fun makeBlockchainInfrastructure(config: Configuration): BlockchainInfrastructure {
        return BaseBlockchainInfrastructure(
                config,
                TestSynchronizationInfrastructure(),
                BaseApiInfrastructure(config))
    }

    override fun makeProcessManager(config: Configuration, blockchainInfrastructure: BlockchainInfrastructure): BlockchainProcessManager {
        return BaseBlockchainProcessManager(
                blockchainInfrastructure, config
        )
    }
}