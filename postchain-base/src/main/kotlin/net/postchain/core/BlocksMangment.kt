package net.postchain.core

import nl.komponents.kovenant.Promise

interface BlockWitnessBuilder {
    fun isComplete(): Boolean
    fun getWitness(): BlockWitness // throws when not complete
}

interface MultiSigBlockWitnessBuilder : BlockWitnessBuilder {
    fun getMySignature(): Signature
    fun applySignature(s: Signature)
}

interface BlockStore {
    fun beginBlock(ctx: EContext): InitialBlockData
    fun addTransaction(bctx: BlockEContext, tx: Transaction): TxEContext
    fun finalizeBlock(bctx: BlockEContext, bh: BlockHeader)
    fun commitBlock(bctx: BlockEContext, w: BlockWitness?)
    fun getBlockHeight(ctx: EContext, blockRID: ByteArray): Long? // returns null if not found
    fun getBlockRIDs(ctx: EContext, height: Long): List<ByteArray> // returns null if height is out of range
    fun getLastBlockHeight(ctx: EContext): Long // height of the last block, first block has height 0
    fun getLastBlockTimestamp(ctx: EContext): Long
    //    fun getBlockData(ctx: EContext, blockRID: ByteArray): BlockData
    fun getWitnessData(ctx: EContext, blockRID: ByteArray): ByteArray

    fun getBlockHeader(ctx: EContext, blockRID: ByteArray): ByteArray

    fun getTxRIDsAtHeight(ctx: EContext, height: Long): Array<ByteArray>
    fun getTxBytes(ctx: EContext, txRID: ByteArray): ByteArray?
    fun getBlockTransactions(ctx: EContext, blockRID: ByteArray): List<ByteArray>

    fun isTransactionConfirmed(ctx: EContext, txRID: ByteArray): Boolean
    fun getConfirmationProofMaterial(ctx: EContext, txRID: ByteArray): Any
}

interface BlockQueries {
    fun getBlockSignature(blockRID: ByteArray): Promise<Signature, Exception>
    fun getBestHeight(): Promise<Long, Exception>
    fun getBlockRids(height: Long): Promise<List<ByteArray>, Exception>
    fun getBlockAtHeight(height: Long): Promise<BlockDataWithWitness, Exception>
    fun getBlockHeader(blockRID: ByteArray): Promise<BlockHeader, Exception>

    fun getBlockTransactionRids(blockRID: ByteArray): Promise<List<ByteArray>, Exception>
    fun getTransaction(txRID: ByteArray): Promise<Transaction?, Exception>
    fun query(query: String): Promise<String, Exception>
    fun isTransactionConfirmed(txRID: ByteArray): Promise<Boolean, Exception>
}

interface BlockBuilder {
    fun begin()
    fun appendTransaction(tx: Transaction)
    fun finalizeBlock()
    fun finalizeAndValidate(blockHeader: BlockHeader)
    fun getBlockData(): BlockData
    fun getBlockWitnessBuilder(): BlockWitnessBuilder?
    fun commit(blockWitness: BlockWitness?)
}

/**
 * A block builder which automatically manages the connection
 */
interface ManagedBlockBuilder : BlockBuilder {
    fun maybeAppendTransaction(tx: Transaction): Exception?
    fun rollback()
}

interface BlockBuildingStrategy {
    fun shouldBuildBlock(): Boolean
    fun blockCommitted(blockData: BlockData)
    fun shouldStopBuildingBlock(bb: BlockBuilder): Boolean
}
