package net.postchain.base

import net.postchain.StorageBuilder
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.base.data.BaseTransactionQueue
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.*
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvFactory

class BaseBlockchainInfrastructure(
        private val nodeConfigProvider: NodeConfigurationProvider,
        val synchronizationInfrastructure: SynchronizationInfrastructure,
        val apiInfrastructure: ApiInfrastructure
) : BlockchainInfrastructure {

    val cryptoSystem = SECP256K1CryptoSystem()
    val blockSigMaker: SigMaker
    val subjectID: ByteArray

    init {
        val privKey = nodeConfigProvider.getConfiguration().privKeyByteArray
        val pubKey = secp256k1_derivePubKey(privKey)
        blockSigMaker = cryptoSystem.buildSigMaker(pubKey, privKey)
        subjectID = pubKey
    }

    override fun shutdown() {
        synchronizationInfrastructure.shutdown()
        apiInfrastructure.shutdown()
    }

    override fun makeBlockchainConfiguration(rawConfigurationData: ByteArray, context: BlockchainContext): BlockchainConfiguration {
        val actualContext = if (context.nodeRID == null) {
            BaseBlockchainContext(context.blockchainRID, context.nodeID, context.chainID, subjectID)
        } else {
            context
        }

        val gtxData = GtvFactory.decodeGtv(rawConfigurationData)
        val confData = BaseBlockchainConfigurationData(gtxData as GtvDictionary, actualContext, blockSigMaker)

        val bcfClass = Class.forName(confData.data["configurationfactory"]!!.asString())
        val factory = (bcfClass.newInstance() as BlockchainConfigurationFactory)

        return factory.makeBlockchainConfiguration(confData)
    }

    override fun makeBlockchainEngine(configuration: BlockchainConfiguration, restartHandler: RestartHandler): BaseBlockchainEngine {
        val storage = StorageBuilder.buildStorage(nodeConfigProvider.getConfiguration().appConfig, NODE_ID_TODO)
        // TODO: [et]: Maybe extract 'queuecapacity' param from ''
        val transactionQueue = BaseTransactionQueue(
                (configuration as BaseBlockchainConfiguration)
                        .configData.getBlockBuildingStrategy()?.get("queuecapacity")?.asInteger()?.toInt() ?: 2500)

        return BaseBlockchainEngine(configuration, storage, configuration.chainID, transactionQueue)
                .apply {
                    setRestartHandler(restartHandler)
                    initializeDB()
                }
    }

    override fun makeBlockchainProcess(processName: String, engine: BlockchainEngine): BlockchainProcess {
        return synchronizationInfrastructure.makeBlockchainProcess(processName, engine)
                .also(apiInfrastructure::connectProcess)
    }

    override fun makeStorage(): Storage {
        return StorageBuilder.buildStorage(nodeConfigProvider.getConfiguration().appConfig, NODE_ID_TODO)
    }
}
