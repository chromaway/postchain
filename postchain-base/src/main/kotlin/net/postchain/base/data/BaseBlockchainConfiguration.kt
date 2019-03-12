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
    val blockchainRID = configData.context.blockchainRID

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
        val signerPubKeys = configData.getSigners()
        return createBlockBuilderInstance(
                cryptoSystem,
                ctx,
                blockStore,
                getTransactionFactory(),
                signerPubKeys.toTypedArray(),
                configData.blockSigMaker)
    }

    open fun createBlockBuilderInstance(cryptoSystem: CryptoSystem,
                                        ctx: EContext,
                                        blockStore: BlockStore,
                                        transactionFactory: TransactionFactory,
                                        signers: Array<ByteArray>,
                                        blockSigMaker: SigMaker
    ): BlockBuilder {
        return BaseBlockBuilder(
                cryptoSystem, ctx, blockStore, getTransactionFactory(), signers, blockSigMaker)
    }

    override fun makeBlockQueries(storage: Storage): BlockQueries {
        return BaseBlockQueries(
                this, storage, blockStore, chainID, configData.subjectID)
    }

    override fun initializeDB(ctx: EContext) {
        blockStore.initialize(ctx, blockchainRID)
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

