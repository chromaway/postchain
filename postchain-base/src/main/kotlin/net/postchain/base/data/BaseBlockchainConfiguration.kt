// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.data

import net.postchain.base.*
import net.postchain.core.*
import net.postchain.getBFTRequiredSignatureCount

open class BaseBlockchainConfiguration(val configData: BaseBlockchainConfigurationData)
    : BlockchainConfiguration {

    override val traits = setOf<String>()
    val cryptoSystem = SECP256K1CryptoSystem()
    val blockStore = BaseBlockStore()
    override val chainID = configData.context.chainID
    override val blockchainRid = configData.context.blockchainRID
    override val effectiveBlockchainRID = configData.getHistoricBRID() ?: configData.context.blockchainRID
    val signers = configData.getSigners()

    val bcRelatedInfosDependencyList: List<BlockchainRelatedInfo> = configData.getDependenciesAsList()

    override fun decodeBlockHeader(rawBlockHeader: ByteArray): BlockHeader {
        return BaseBlockHeader(rawBlockHeader, cryptoSystem)
    }

    override fun decodeWitness(rawWitness: ByteArray): BlockWitness {
        return BaseBlockWitness.fromBytes(rawWitness)
    }

    // This is basically a duplicate of BaseBlockBuilder.validateWitness.
    // We should find a common place to put this code.
    fun verifyBlockHeader(blockHeader: BlockHeader, blockWitness: BlockWitness): Boolean {
        if (!(blockWitness is MultiSigBlockWitness)) {
            throw ProgrammerMistake("Invalid BlockWitness implementation.")
        }
        val signers = signers.toTypedArray()
        val witnessBuilder = BaseBlockWitnessBuilder(cryptoSystem, blockHeader, signers, getBFTRequiredSignatureCount(signers.size))
        for (signature in blockWitness.getSignatures()) {
            witnessBuilder.applySignature(signature)
        }
        return witnessBuilder.isComplete()
    }

    override fun getTransactionFactory(): TransactionFactory {
        return BaseTransactionFactory()
    }

    open fun getSpecialTxHandler(): SpecialTransactionHandler {
        return NullSpecialTransactionHandler()
    }

    override fun makeBlockBuilder(ctx: EContext): BlockBuilder {
        addChainIDToDependencies(ctx) // We wait until now with this, b/c now we have an EContext
        return BaseBlockBuilder(
                effectiveBlockchainRID,
                cryptoSystem,
                ctx,
                blockStore,
                getTransactionFactory(),
                getSpecialTxHandler(),
                signers.toTypedArray(),
                configData.blockSigMaker,
                bcRelatedInfosDependencyList,
                effectiveBlockchainRID != blockchainRid,
                configData.getMaxBlockSize(),
                configData.getMaxBlockTransactions())
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
        DependenciesValidator.validateBlockchainRids(ctx, bcRelatedInfosDependencyList)
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

