// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

import net.postchain.StorageBuilder
import net.postchain.base.BaseBlockchainConfigurationData.Companion.KEY_CONFIGURATIONFACTORY
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.base.data.BaseTransactionQueue
import net.postchain.base.data.DatabaseAccess
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.*
import net.postchain.debug.BlockchainProcessName
import net.postchain.debug.NodeDiagnosticContext
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvFactory

open class BaseBlockchainInfrastructure(
        private val nodeConfigProvider: NodeConfigurationProvider,
        val synchronizationInfrastructure: SynchronizationInfrastructure,
        val apiInfrastructure: ApiInfrastructure,
        val nodeDiagnosticContext: NodeDiagnosticContext
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

    /**
     * Builds a [BlockchainConfiguration] instance from the given components
     *
     * @param rawConfigurationData is the byte array with the configuration.
     * @param eContext is the DB context
     * @param nodeId
     * @param chainId
     * @param initialBlockchainRID is null or a blokchain RID
     * @return the newly created [BlockchainConfiguration]
     */
    override fun makeBlockchainConfiguration(
            rawConfigurationData: ByteArray,
            eContext: EContext,
            nodeId: Int,
            chainId: Long
    ): BlockchainConfiguration {

        val gtvData = GtvFactory.decodeGtv(rawConfigurationData)
        val brid = DatabaseAccess.of(eContext).getBlockchainRid(eContext)!!

        val context = BaseBlockchainContext(brid, nodeId, chainId, subjectID)
        val confData = BaseBlockchainConfigurationData(gtvData as GtvDictionary, context, blockSigMaker)

        val bcfClass = Class.forName(confData.data[KEY_CONFIGURATIONFACTORY]!!.asString())
        val factory = (bcfClass.newInstance() as BlockchainConfigurationFactory)

        val config = factory.makeBlockchainConfiguration(confData)
        config.initializeDB(eContext)

        return config
    }

    override fun makeBlockchainEngine(
            processName: BlockchainProcessName,
            configuration: BlockchainConfiguration,
            restartHandler: RestartHandler
    ): BaseBlockchainEngine {

        val storage = StorageBuilder.buildStorage(
                nodeConfigProvider.getConfiguration().appConfig, NODE_ID_TODO)

        // TODO: [et]: Maybe extract 'queuecapacity' param from ''
        val transactionQueue = BaseTransactionQueue(
                (configuration as BaseBlockchainConfiguration)
                        .configData.getBlockBuildingStrategy()?.get("queuecapacity")?.asInteger()?.toInt() ?: 2500)

        return BaseBlockchainEngine(processName, configuration, storage, configuration.chainID, transactionQueue)
                .apply {
                    setRestartHandler(restartHandler)
                    initialize()
                }
    }

    override fun makeBlockchainProcess(processName: BlockchainProcessName, engine: BlockchainEngine,
                                       historicBlockchain: HistoricBlockchain?): BlockchainProcess {
        return synchronizationInfrastructure.makeBlockchainProcess(processName, engine, historicBlockchain)
                .also(apiInfrastructure::connectProcess)
    }

}
