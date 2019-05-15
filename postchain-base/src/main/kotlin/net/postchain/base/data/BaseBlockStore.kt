// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.base.data

import mu.KLogging
import net.postchain.base.*
import net.postchain.base.merkle.Hash
import net.postchain.core.*

/**
 * Provides database access to the location where the blockchain with related metadata and transactions
 * are stored
 *
 * @property db Object used to access the DBMS
 */
class BaseBlockStore : BlockStore {
    var db: DatabaseAccess = SQLDatabaseAccess()

    companion object: KLogging()


    /**
     * Get initial block data, i.e. data necessary for building the next block
     *
     * @param ctx Connection context
     * @returns Initial block data
     */
    override fun beginBlock(ctx: EContext, blockHeightDependencies: Array<Hash?>?): InitialBlockData {
        if (ctx.chainID < 0) {
            throw UserMistake("ChainId must be >=0, got ${ctx.chainID}")
        }
        val prevHeight = getLastBlockHeight(ctx)
        val prevTimestamp = getLastBlockTimestamp(ctx)
        val blockchainRID: ByteArray = db.getBlockchainRID(ctx)
                    ?: throw UserMistake("Blockchain RID not found for chainId ${ctx.chainID}")
        val prevBlockRID = if (prevHeight == -1L) {
            blockchainRID
        } else {
            getBlockRID(ctx, prevHeight) ?: throw ProgrammerMistake("Previous block had no RID. Check your block writing code!")
        }

        val blockIid = db.insertBlock(ctx, prevHeight + 1)
        return InitialBlockData(blockchainRID, blockIid, ctx.chainID, prevBlockRID, prevHeight + 1, prevTimestamp, blockHeightDependencies)
    }

    override fun addTransaction(bctx: BlockEContext, tx: Transaction): TxEContext {
        val txIid = db.insertTransaction(bctx, tx)
        return BaseTxEContext(bctx, txIid)
    }

    override fun finalizeBlock(bctx: BlockEContext, bh: BlockHeader) {
        db.finalizeBlock(bctx, bh)
    }


    override fun commitBlock(bctx: BlockEContext, w: BlockWitness?) {
        if (w == null) return
        db.commitBlock(bctx, w)
    }

    override fun getBlockHeightFromOwnBlockchain(ctx: EContext, blockRID: ByteArray): Long? {
        return db.getBlockHeight(ctx, blockRID, ctx.chainID)
    }

    override fun getBlockHeightFromAnyBlockchain(ctx: EContext, blockRID: ByteArray, chainId: Long): Long? {
        return db.getBlockHeight(ctx, blockRID, chainId)
    }

    override fun getChainId(ctx: EContext, blockchainRID: ByteArray): Long? {
        return db.getChainId(ctx, blockchainRID)
    }

    override fun getBlockRID(ctx: EContext, height: Long): ByteArray? {
        return db.getBlockRID(ctx, height)
    }

    override fun getBlockHeader(ctx: EContext, blockRID: ByteArray): ByteArray {
        return db.getBlockHeader(ctx, blockRID)
    }

    // This implementation does not actually *stream* data from the database connection.
    // It is buffered in an ArrayList by ArrayListHandler() which is unfortunate.
    // Eventually, we may change this implementation to actually deliver a true
    // stream so that we don't have to store all transaction data in memory.
    override fun getBlockTransactions(ctx: EContext, blockRID: ByteArray): List<ByteArray> {
        return db.getBlockTransactions(ctx, blockRID)
    }

    override fun getWitnessData(ctx: EContext, blockRID: ByteArray): ByteArray {
        return db.getWitnessData(ctx, blockRID)
    }

    override fun getLastBlockHeight(ctx: EContext): Long {
        return db.getLastBlockHeight(ctx)
    }

    override fun getBlockHeightInfo(ctx: EContext, blockchainRID: ByteArray): Pair<Long, Hash>? {
        return db.getBlockHeightInfo(ctx, blockchainRID)
    }

    override fun getLastBlockTimestamp(ctx: EContext): Long {
        return db.getLastBlockTimestamp(ctx)
    }

    override fun getTxRIDsAtHeight(ctx: EContext, height: Long): Array<ByteArray> {
        return db.getTxRIDsAtHeight(ctx, height)
    }

    override fun getConfirmationProofMaterial(ctx: EContext, txRID: ByteArray): Any {
        val block = db.getBlockInfo(ctx, txRID)
        return ConfirmationProofMaterial(
                ByteArrayKey(db.getTxHash(ctx, txRID)),
                db.getBlockTxHashes(ctx, block.blockIid).map{ ByteArrayKey(it) }.toTypedArray(),
                block.blockHeader,
                block.witness
        )
    }

    override fun getTxBytes(ctx: EContext, txRID: ByteArray): ByteArray? {
        return db.getTxBytes(ctx, txRID)
    }

    override fun isTransactionConfirmed(ctx: EContext, txRID: ByteArray): Boolean {
        return db.isTransactionConfirmed(ctx, txRID)
    }

    fun initialize(ctx: EContext, blockchainRID: ByteArray, dependencies:  List<BlockchainRelatedInfo>) {
        db.checkBlockchainRID(ctx, blockchainRID)

        // Verify all dependencies
        for (dep in dependencies) {
            val chainId = db.getChainId(ctx, dep.blockchainRid)
            if (chainId == null) {
                throw BadDataMistake(BadDataType.BAD_CONFIGURATION,
                        "Dependency given in configuration: ${dep.nickname} is missing in DB. Dependent blockchains must be added in correct order!")
            } else {
                logger.info("initialize() - Verified BC dependency: ${dep.nickname} exists as chainID: = $chainId (before: ${dep.chainId}) ")
                dep.chainId = chainId
            }
        }
    }
}
