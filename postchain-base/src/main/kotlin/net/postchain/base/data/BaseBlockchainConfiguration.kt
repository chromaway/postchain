// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.base.data

import net.postchain.base.*
import net.postchain.core.*

open class BaseBlockchainConfiguration(val configData: BaseBlockchainConfigurationData)
    : BlockchainConfiguration {

    override val traits = setOf<String>()
    val cryptoSystem = SECP256K1CryptoSystem()
    val blockStore = BaseBlockStore()
    override val chainID = configData.context.chainID
    override val blockchainRID = configData.context.blockchainRID
    val signers = configData.getSigners()

    val bcRelatedInfosDependencyList: List<BlockchainRelatedInfo> = configData.getDependenciesAsList()

    override fun decodeBlockHeader(rawBlockHeader: ByteArray): BlockHeader {
        return BaseBlockHeader(rawBlockHeader, cryptoSystem)
    }

    override fun decodeWitness(rawWitness: ByteArray): BlockWitness {
        return BaseBlockWitness.fromBytes(rawWitness)
    }

    override fun getTransactionFactory(): TransactionFactory {
        return BaseTransactionFactory()
    }

    override fun makeBlockBuilder(ctx: EContext): BlockBuilder {
        addChainIDToDependencies(ctx) // We wait until now with this, b/c now we have an EContext
        return BaseBlockBuilder(
                blockchainRID,
                cryptoSystem,
                ctx,
                blockStore,
                getTransactionFactory(),
                signers.toTypedArray(),
                configData.blockSigMaker,
                bcRelatedInfosDependencyList)
    }

    /**
     * Will add ChainID to the dependency list, if needed.
     */
    @Synchronized
    private fun addChainIDToDependencies(ctx: EContext) {
        if (bcRelatedInfosDependencyList.isNotEmpty()) {
            // Check if we have added ChainId's already
            val first = bcRelatedInfosDependencyList.first()
            if (first.chainId == null) {
                // We have to fill up the cache of ChainIDs
                for (bcInfo in bcRelatedInfosDependencyList) {
                    val depChainId = blockStore.getChainId(ctx, bcInfo.blockchainRid)
                    bcInfo.chainId = depChainId ?: throw BadDataMistake(BadDataType.BAD_CONFIGURATION,
                            "The blockchain configuration claims we depend on: $bcInfo so this BC must exist in DB"
                                    + "(Order is wrong. It must have been configured BEFORE this point in time)")
                }
            }
        }
    }

    override fun makeBlockQueries(storage: Storage): BlockQueries {
        return BaseBlockQueries(
                this, storage, blockStore, chainID, configData.subjectID)
    }

    override fun initializeDB(ctx: EContext) {
        blockStore.initialValidation(ctx, bcRelatedInfosDependencyList)
    }

    override fun getBlockBuildingStrategy(blockQueries: BlockQueries, txQueue: TransactionQueue): BlockBuildingStrategy {
        val strategyClassName = configData.getBlockBuildingStrategyName()
        if (strategyClassName == "") {
            return BaseBlockBuildingStrategy(configData, this, blockQueries, txQueue)
        }
        val strategyClass = Class.forName(strategyClassName)

        val ctor = strategyClass.getConstructor(
                BaseBlockchainConfigurationData::class.java,
                BlockchainConfiguration::class.java,
                BlockQueries::class.java,
                TransactionQueue::class.java)

        return ctor.newInstance(configData, this, blockQueries, txQueue) as BlockBuildingStrategy
    }
}

