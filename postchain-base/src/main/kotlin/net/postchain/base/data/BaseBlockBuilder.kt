// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.data

import mu.KLogging
import net.postchain.base.*
import net.postchain.base.merkle.Hash
import net.postchain.common.toHex
import net.postchain.core.*
import net.postchain.getBFTRequiredSignatureCount
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
import net.postchain.gtv.merkleHash
import java.lang.Long.max
import java.util.*

/**
 * BaseBlockBuilder is used to aid in building blocks, including construction and validation of block header and witness
 *
 * @property cryptoSystem Crypto utilities
 * @property eContext Connection context including blockchain and node identifiers
 * @property store For database access
 * @property txFactory Used for serializing transaction data
 * @property subjects Public keys for nodes authorized to sign blocks
 * @property blockchainDependencies holds the blockchain RIDs this blockchain depends on
 * @property blockSigMaker used to produce signatures on blocks for local node
 */
open class BaseBlockBuilder(
        blockchainRID: BlockchainRid,
        val cryptoSystem: CryptoSystem,
        eContext: EContext,
        store: BlockStore,
        txFactory: TransactionFactory,
        val specialTxHandler: SpecialTransactionHandler,
        val subjects: Array<ByteArray>,
        val blockSigMaker: SigMaker,
        val blockchainRelatedInfoDependencyList: List<BlockchainRelatedInfo>,
        val usingHistoricBRID: Boolean,
        val maxBlockSize: Long = 20 * 1024 * 1024, // 20mb
        val maxBlockTransactions: Long = 100
): AbstractBlockBuilder(eContext, blockchainRID, store, txFactory) {

    companion object : KLogging()

    private val calc = GtvMerkleHashCalculator(cryptoSystem)

    private var blockSize: Long = 0L
    private var haveSpecialEndTransaction = false

    /**
     * Computes the root hash for the Merkle tree of transactions currently in a block
     *
     * @return The Merkle tree root hash
     */
    fun computeMerkleRootHash(): ByteArray {
        val digests = rawTransactions.map { txFactory.decodeTransaction(it).getHash() }

        val gtvArr = gtv(digests.map { gtv(it) })

        return gtvArr.merkleHash(calc)
    }

    override fun begin(partialBlockHeader: BlockHeader?) {
        if (partialBlockHeader == null && usingHistoricBRID) {
            throw UserMistake("Cannot build new blocks in historic mode (check configuration)")
        }
        super.begin(partialBlockHeader)
        if (buildingNewBlock
                && specialTxHandler.needsSpecialTransaction(SpecialTransactionPosition.Begin)) {
            appendTransaction(specialTxHandler.createSpecialTransaction(
                    SpecialTransactionPosition.Begin,
                    bctx
            ))
        }
    }

    /**
     * Create block header from initial block data
     *
     * @return Block header
     */
    override fun makeBlockHeader(): BlockHeader {
        // If our time is behind the timestamp of most recent block, do a minimal increment
        val timestamp = max(System.currentTimeMillis(), initialBlockData.timestamp + 1)
        val rootHash = computeMerkleRootHash()
        return BaseBlockHeader.make(cryptoSystem, initialBlockData, rootHash, timestamp)
    }

    /**
     * Validate block header:
     * - check that previous block RID is used in this block
     * - check for correct height
     * - check that timestamp occurs after previous blocks timestamp
     * - check if all required dependencies are present
     * - check for correct root hash
     *
     * @param blockHeader The block header to validate
     */
    override fun validateBlockHeader(blockHeader: BlockHeader): ValidationResult {
        val header = blockHeader as BaseBlockHeader

        val computedMerkleRoot = computeMerkleRootHash()
        return when {
            !header.prevBlockRID.contentEquals(initialBlockData.prevBlockRID) ->
                ValidationResult(false, "header.prevBlockRID != initialBlockData.prevBlockRID," +
                        "( ${header.prevBlockRID.toHex()} != ${initialBlockData.prevBlockRID.toHex()} ), " +
                        " height: ${header.blockHeaderRec.getHeight()} and ${initialBlockData.height} ")

            header.blockHeaderRec.getHeight() != initialBlockData.height ->
                ValidationResult(false, "header.blockHeaderRec.height != initialBlockData.height")

            bctx.timestamp >= header.timestamp ->
                ValidationResult(false, "bctx.timestamp >= header.timestamp")

            !header.checkIfAllBlockchainDependenciesArePresent(blockchainRelatedInfoDependencyList) ->
                ValidationResult(false, "checkIfAllBlockchainDependenciesArePresent() is false")

            !Arrays.equals(header.blockHeaderRec.getMerkleRootHash(), computedMerkleRoot) -> // Do this last since most expensive check!
                ValidationResult(false, "header.blockHeaderRec.rootHash != computeRootHash()")

            else -> ValidationResult(true)
        }
    }

    /**
     * Validates the following:
     *  - Witness is of a correct implementation
     *  - The signatures are valid with respect to the block being signed
     *  - The number of signatures exceeds the threshold necessary to deem the block itself valid
     *
     *  @param blockWitness The witness to be validated
     *  @throws ProgrammerMistake Invalid BlockWitness implementation
     */
    override fun validateWitness(blockWitness: BlockWitness): Boolean {
        if (!(blockWitness is MultiSigBlockWitness)) {
            throw ProgrammerMistake("Invalid BlockWitness impelmentation.")
        }
        val witnessBuilder = BaseBlockWitnessBuilder(cryptoSystem, _blockData!!.header, subjects, getBFTRequiredSignatureCount(subjects.size))
        for (signature in blockWitness.getSignatures()) {
            witnessBuilder.applySignature(signature)
        }
        return witnessBuilder.isComplete()
    }

    /**
     * @param partialBlockHeader if this is given, we should get the dependency information from the header, else
     *                           we should get the heights from the DB.
     * @return all dependencies to other blockchains and their heights this block needs.
     */
    override fun buildBlockchainDependencies(partialBlockHeader: BlockHeader?): BlockchainDependencies {
        return if (partialBlockHeader != null) {
            buildBlockchainDependenciesFromHeader(partialBlockHeader)
        } else {
            buildBlockchainDependenciesFromDb()
        }
    }

    private fun buildBlockchainDependenciesFromHeader(partialBlockHeader: BlockHeader): BlockchainDependencies {
        return if (blockchainRelatedInfoDependencyList.isNotEmpty()) {

            val baseBH = partialBlockHeader as BaseBlockHeader
            val givenDependencies = baseBH.blockHeightDependencyArray
            if (givenDependencies.size == blockchainRelatedInfoDependencyList.size) {

                val resList = mutableListOf<BlockchainDependency>()
                var i = 0
                for (bcInfo in blockchainRelatedInfoDependencyList) {
                    val blockRid = givenDependencies[i]
                    val dep = if (blockRid != null) {
                        val dbHeight = store.getBlockHeightFromAnyBlockchain(bctx, blockRid, bcInfo.chainId!!)
                        if (dbHeight != null) {
                            BlockchainDependency(bcInfo, HeightDependency(blockRid, dbHeight))
                        } else {
                            // Ok to bang out if we are behind in blocks. Discussed this with Alex (2019-03-29)
                            throw BadDataMistake(BadDataType.MISSING_DEPENDENCY,
                                    "We are not ready to accept the block since block dependency (RID: ${blockRid.toHex()}) is missing. ")
                        }
                    } else {
                        BlockchainDependency(bcInfo, null) // No blocks required -> allowed
                    }
                    resList.add(dep)
                    i++
                }
                BlockchainDependencies(resList)
            } else {
                throw BadDataMistake(BadDataType.BAD_CONFIGURATION,
                        "The given block header has ${givenDependencies.size} dependencies our configuration requires ${blockchainRelatedInfoDependencyList.size} ")
            }
        } else {
            BlockchainDependencies(listOf()) // No dependencies
        }
    }

    private fun buildBlockchainDependenciesFromDb(): BlockchainDependencies {
        val resList = mutableListOf<BlockchainDependency>()
        for (bcInfo in blockchainRelatedInfoDependencyList) {
            val res: Pair<Long, Hash>? = store.getBlockHeightInfo(ectx, bcInfo.blockchainRid)
            val dep = if (res != null) {
                BlockchainDependency(bcInfo, HeightDependency(res.second, res.first))
            } else {
                BlockchainDependency(bcInfo, null) // No blocks yet, it's ok
            }
            resList.add(dep)
        }
        return BlockchainDependencies(resList)
    }

    /**
     * Retrieve the builder for block witnesses. It can only be retrieved if the block is finalized.
     *
     * @return The block witness builder
     * @throws ProgrammerMistake If the block is not finalized yet signatures can't be created since they would
     * be invalid when further transactions are added to the block
     */
    override fun getBlockWitnessBuilder(): BlockWitnessBuilder? {
        if (!finalized) {
            throw ProgrammerMistake("Block is not finalized yet.")
        }

        val witnessBuilder = BaseBlockWitnessBuilder(cryptoSystem, _blockData!!.header, subjects, getBFTRequiredSignatureCount(subjects.size))
        witnessBuilder.applySignature(blockSigMaker.signDigest(_blockData!!.header.blockRID)) // TODO: POS-04_sig
        return witnessBuilder
    }

    override fun finalizeBlock(): BlockHeader {
        if (buildingNewBlock && specialTxHandler.needsSpecialTransaction(SpecialTransactionPosition.End))
            appendTransaction(specialTxHandler.createSpecialTransaction(SpecialTransactionPosition.End, bctx))
        return super.finalizeBlock()
    }

    override fun finalizeAndValidate(blockHeader: BlockHeader) {
        if (specialTxHandler.needsSpecialTransaction(SpecialTransactionPosition.End) && !haveSpecialEndTransaction)
            throw BlockValidationMistake("End special transaction is missing")
        super.finalizeAndValidate(blockHeader)
    }

    private fun checkSpecialTransaction(tx: Transaction) {
        if (transactions.size == 0) {
            // first transaction
            if (specialTxHandler.needsSpecialTransaction(SpecialTransactionPosition.Begin)) {
                if (!specialTxHandler.validateSpecialTransaction(SpecialTransactionPosition.Begin,
                                tx, bctx))
                    throw BlockValidationMistake("Special transaction validation failed")
                else
                    return // all is well, the first transaction is special and valid
            }
        }
        if (tx.isSpecial()) {
            // if tx is special it must be the end transaction
            if (!specialTxHandler.needsSpecialTransaction(SpecialTransactionPosition.End))
                throw BlockValidationMistake("Found unexpected special transaction")
            if (!specialTxHandler.validateSpecialTransaction(SpecialTransactionPosition.End,
                            tx, bctx))
                throw BlockValidationMistake("Special transaction validation failed")
            haveSpecialEndTransaction = true
        }
    }

    override fun appendTransaction(tx: Transaction) {
        if (!buildingNewBlock) {
            if (haveSpecialEndTransaction) throw BlockValidationMistake("Cannot append transactions after end special transaction")
            checkSpecialTransaction(tx)
        }
        super.appendTransaction(tx)
        blockSize += tx.getRawData().size
        if (blockSize >= maxBlockSize) {
            throw UserMistake("block size exceeds max block size ${maxBlockSize} bytes")
        } else if (transactions.size >= maxBlockTransactions) {
            throw UserMistake("Number of transactions exceeds max ${maxBlockTransactions} transactions in block")
        }
    }

}