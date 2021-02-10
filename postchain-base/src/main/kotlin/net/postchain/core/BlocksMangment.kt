// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.core

import net.postchain.base.BlockchainRid
import net.postchain.base.merkle.Hash
import net.postchain.gtv.Gtv
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
    fun beginBlock(ctx: EContext, blockchainRID: BlockchainRid, blockHeightDependencies: Array<Hash?>?): InitialBlockData
    fun addTransaction(bctx: BlockEContext, tx: Transaction): TxEContext
    fun finalizeBlock(bctx: BlockEContext, bh: BlockHeader)
    fun commitBlock(bctx: BlockEContext, w: BlockWitness)
    fun getBlockHeightFromOwnBlockchain(ctx: EContext, blockRID: ByteArray): Long? // returns null if not found
    fun getBlockHeightFromAnyBlockchain(ctx: EContext, blockRID: ByteArray, chainId: Long): Long? // returns null if not found
    fun getChainId(ctx: EContext, blockchainRID: BlockchainRid): Long? // returns null if not found
    fun getBlockRID(ctx: EContext, height: Long): ByteArray? // returns null if height is out of range
    fun getLastBlockHeight(ctx: EContext): Long // height of the last block, first block has height 0
    fun getBlockHeightInfo(ctx: EContext, blockchainRID: BlockchainRid): Pair<Long, Hash>?
    fun getLastBlockTimestamp(ctx: EContext): Long
    //    fun getBlockData(ctx: EContext, blockRID: ByteArray): BlockData
    fun getWitnessData(ctx: EContext, blockRID: ByteArray): ByteArray

    fun getBlocks(ctx: EContext, beforeTime: Long, limit: Int, partialTx: Boolean): List<BlockDetail>
    fun getBlock(ctx: EContext, blockRID: ByteArray, partialTx: Boolean): BlockDetail?
    fun getTransactionInfo(ctx: EContext, txRID: ByteArray): TransactionInfoExt?
    fun getTransactionsInfo(ctx: EContext, beforeTime: Long, limit: Int): List<TransactionInfoExt>

    fun getBlockHeader(ctx: EContext, blockRID: ByteArray): ByteArray
    fun getTxRIDsAtHeight(ctx: EContext, height: Long): Array<ByteArray>
    fun getTxBytes(ctx: EContext, txRID: ByteArray): ByteArray?
    fun getBlockTransactions(ctx: EContext, blockRID: ByteArray): List<ByteArray>

    fun isTransactionConfirmed(ctx: EContext, txRID: ByteArray): Boolean
    fun getConfirmationProofMaterial(ctx: EContext, txRID: ByteArray): Any
}

/**
 * A collection of methods for various blockchain related queries
 */
interface BlockQueries {
    fun getBlockSignature(blockRID: ByteArray): Promise<Signature, Exception>
    fun getBestHeight(): Promise<Long, Exception>
    fun getBlockRid(height: Long): Promise<ByteArray?, Exception>
    fun getBlockAtHeight(height: Long, includeTransactions: Boolean = true): Promise<BlockDataWithWitness?, Exception>
    fun getBlockHeader(blockRID: ByteArray): Promise<BlockHeader, Exception>
    fun getBlocks(beforeTime: Long, limit: Int, partialTx: Boolean): Promise<List<BlockDetail>, Exception>
    fun getBlock(blockRID: ByteArray, partialTx: Boolean): Promise<BlockDetail?, Exception>

    fun getBlockTransactionRids(blockRID: ByteArray): Promise<List<ByteArray>, Exception>
    fun getTransaction(txRID: ByteArray): Promise<Transaction?, Exception>
    fun getTransactionInfo(txRID: ByteArray): Promise<TransactionInfoExt?, Exception>
    fun getTransactionsInfo(beforeTime: Long, limit: Int): Promise<List<TransactionInfoExt>, Exception>
    fun query(query: String): Promise<String, Exception>
    fun query(name: String, args: Gtv): Promise<Gtv, Exception>
    fun isTransactionConfirmed(txRID: ByteArray): Promise<Boolean, Exception>
}

/**
 * Builds one block, either:
 *  1. a block we define ourselves (we are the Primary Node = block builder) or
 *  2. an externally produced block (we loading the finished block from the Primary Node).
 *
 * The life cycle of the [BlockBuilder] is:
 * 1. begin()
 * 2. appendTransaction() <- once per TX
 * 3. finalizeBlock()
 * 4. getBlockWitnessBuilder() <- Applies signatures
 * 5. getBlockData()
 *
 * (For more documentation, see sub classes)
 */
interface BlockBuilder {
    fun begin(partialBlockHeader: BlockHeader?)
    fun appendTransaction(tx: Transaction)
    fun finalizeBlock(): BlockHeader
    fun finalizeAndValidate(blockHeader: BlockHeader)
    fun getBlockData(): BlockData
    fun getBlockWitnessBuilder(): BlockWitnessBuilder?
    fun commit(blockWitness: BlockWitness)
}

/**
 * A block builder which automatically manages the connection
 */
interface ManagedBlockBuilder : BlockBuilder {
    fun maybeAppendTransaction(tx: Transaction): Exception?
    fun rollback()
}

/**
 * Strategy configurations for how to create new blocks
 */
interface BlockBuildingStrategy {
    fun shouldBuildBlock(): Boolean
    fun blockCommitted(blockData: BlockData)
    fun shouldStopBuildingBlock(bb: BlockBuilder): Boolean
}
