// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.core

import net.postchain.base.BaseBlockEContext
import net.postchain.base.BlockchainDependencies
import net.postchain.base.BlockchainRid
import net.postchain.common.TimeLog
import net.postchain.common.toHex

/**
 * This class includes the bare minimum functionality required by a real block builder
 *
 * @property ectx Connection context including blockchain and node identifiers
 * @property store For database access
 * @property txFactory Used for serializing transaction data
 * @property finalized Boolean signalling if further updates to block is permitted
 * @property rawTransactions list of encoded transactions
 * @property transactions list of decoded transactions
 * @property _blockData complete set of data for the block including header and [rawTransactions]
 * @property initialBlockData
 */
abstract class AbstractBlockBuilder(
        val ectx: EContext,
        val blockchainRID: BlockchainRid,
        val store: BlockStore,
        val txFactory: TransactionFactory
) : BlockBuilder {

    // functions which need to be implemented in a concrete BlockBuilder:
    abstract fun makeBlockHeader(): BlockHeader

    abstract fun validateBlockHeader(blockHeader: BlockHeader): ValidationResult
    abstract fun validateWitness(blockWitness: BlockWitness): Boolean
    abstract fun buildBlockchainDependencies(partialBlockHeader: BlockHeader?): BlockchainDependencies
    // fun getBlockWitnessBuilder(): BlockWitnessBuilder?;

    var finalized: Boolean = false
    val rawTransactions = mutableListOf<ByteArray>()
    val transactions = mutableListOf<Transaction>()
    var blockchainDependencies: BlockchainDependencies? = null
    lateinit var bctx: BlockEContext
    lateinit var initialBlockData: InitialBlockData
    var _blockData: BlockData? = null

    /**
     * Retrieve initial block data and set block context
     *
     * @param partialBlockHeader might hold the header.
     */
    override fun begin(partialBlockHeader: BlockHeader?) {
        if (finalized) {
            ProgrammerMistake("This builder has already been used once (you must create a new builder instance)")
        }
        blockchainDependencies = buildBlockchainDependencies(partialBlockHeader)
        initialBlockData = store.beginBlock(ectx, blockchainRID, blockchainDependencies!!.extractBlockHeightDependencyArray())
        bctx = BaseBlockEContext(
                ectx,
                initialBlockData.blockIID,
                initialBlockData.timestamp,
                blockchainDependencies!!.extractChainIdToHeightMap())
    }

    /**
     * Apply transaction to current working block
     *
     * @param tx transaction to be added to block
     * @throws ProgrammerMistake if block is finalized
     * @throws UserMistake transaction is not correct
     * @throws UserMistake failed to save transaction to database
     * @throws UserMistake failed to apply transaction and update database state
     */
    override fun appendTransaction(tx: Transaction) {
        if (finalized) throw ProgrammerMistake("Block is already finalized")
        // tx.isCorrect may also throw UserMistake to provide
        // a meaningful error message to log.
        TimeLog.startSum("AbstractBlockBuilder.appendTransaction().isCorrect")
        if (!tx.isCorrect()) {
            throw UserMistake("Transaction ${tx.getRID().toHex()} is not correct")
        }
        TimeLog.end("AbstractBlockBuilder.appendTransaction().isCorrect")
        val txctx: TxEContext
        try {
            TimeLog.startSum("AbstractBlockBuilder.appendTransaction().addTransaction")
            txctx = store.addTransaction(bctx, tx)
            TimeLog.end("AbstractBlockBuilder.appendTransaction().addTransaction")
        } catch (e: Exception) {
            throw UserMistake("Failed to save tx to database", e)
        }
        // In case of errors, tx.apply may either return false or throw UserMistake
        TimeLog.startSum("AbstractBlockBuilder.appendTransaction().apply")
        if (tx.apply(txctx)) {
            transactions.add(tx)
            rawTransactions.add(tx.getRawData())
        } else {
            throw UserMistake("Transaction ${tx.getRID().toHex()} failed")
        }
        TimeLog.end("AbstractBlockBuilder.appendTransaction().apply")
    }

    /**
     * By finalizing the block we won't allow any more transactions to be added, and the block RID and timestamp are set
     */
    override fun finalizeBlock(): BlockHeader {
        val blockHeader = makeBlockHeader()
        store.finalizeBlock(bctx, blockHeader)
        _blockData = BlockData(blockHeader, rawTransactions)
        finalized = true
        return blockHeader
    }

    /**
     * Apart from finalizing the block, validate the header
     *
     * @param blockHeader Block header to finalize and validate
     * @throws UserMistake Happens if validation of the block header fails
     */
    override fun finalizeAndValidate(blockHeader: BlockHeader) {
        validateBlockHeader(blockHeader).run {
            if (result) {
                store.finalizeBlock(bctx, blockHeader)
                _blockData = BlockData(blockHeader, rawTransactions)
                finalized = true
            } else {
                throw UserMistake("Invalid block header: $message")
            }
        }
    }

    /**
     * Return block data if block is finalized.
     *
     * @throws ProgrammerMistake When block is not finalized
     */
    override fun getBlockData(): BlockData {
        return _blockData ?: throw ProgrammerMistake("Block is not finalized yet")
    }

    /**
     * By commiting to the block we update the database to include the witness for that block
     *
     * @param blockWitness The witness for the block
     * @throws ProgrammerMistake If the witness is invalid
     */
    override fun commit(blockWitness: BlockWitness?) {
        if (blockWitness != null && !validateWitness(blockWitness)) {
            throw ProgrammerMistake("Invalid witness")
        }
        store.commitBlock(bctx, blockWitness)
    }
}
