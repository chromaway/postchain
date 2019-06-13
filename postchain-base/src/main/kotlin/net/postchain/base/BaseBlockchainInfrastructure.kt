package net.postchain.base

import mu.KLogging
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

    companion object: KLogging()


    override fun shutdown() {
        synchronizationInfrastructure.shutdown()
        apiInfrastructure.shutdown()
    }

    override fun parseConfigurationString(rawData: String, format: String): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

    override fun makeBlockchainEngine(configuration: BlockchainConfiguration): BaseBlockchainEngine {
        logger.info("makeBlockchainEngine() - start")
        val storage = StorageBuilder.buildStorage(nodeConfigProvider.getConfiguration(), -1) // TODO: nodeID
        // TODO: [et]: Maybe extract 'queuecapacity' param from ''
        val tq = BaseTransactionQueue(
                (configuration as BaseBlockchainConfiguration)
                        .configData.getBlockBuildingStrategy()?.get("queuecapacity")?.asInteger()?.toInt() ?: 2500)
        logger.info("makeBlockchainEngine() - end")
        return BaseBlockchainEngine(configuration, storage, configuration.chainID, tq)
                .apply { initializeDB() }
    }

    override fun makeBlockchainProcess(processName: String, engine: BlockchainEngine, restartHandler: RestartHandler): BlockchainProcess {
        return synchronizationInfrastructure.makeBlockchainProcess(processName,engine, restartHandler)
                .also(apiInfrastructure::connectProcess)
    }
}
