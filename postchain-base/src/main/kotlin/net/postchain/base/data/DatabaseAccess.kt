// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.data

import net.postchain.base.BlockchainRid
import net.postchain.base.PeerInfo
import net.postchain.core.*
import net.postchain.network.x.XPeerID
import java.sql.Connection
import java.time.Instant

interface DatabaseAccess {

    class BlockInfo(
            val blockIid: Long,
            val blockHeader: ByteArray,
            val witness: ByteArray)

    class BlockInfoExt(
            val blockRid: ByteArray,
            val blockHeight: Long,
            val blockHeader: ByteArray,
            val witness: ByteArray,
            val timestamp: Long)

    fun tableName(ctx: EContext, table: String): String

    fun isSavepointSupported(): Boolean
    fun isSchemaExists(connection: Connection, schema: String): Boolean
    fun createSchema(connection: Connection, schema: String)
    fun setCurrentSchema(connection: Connection, schema: String)
    fun dropSchemaCascade(connection: Connection, schema: String)

    fun initializeApp(connection: Connection, expectedDbVersion: Int)
    fun initializeBlockchain(ctx: EContext, blockchainRid: BlockchainRid)
    fun getChainId(ctx: EContext, blockchainRid: BlockchainRid): Long?
    fun getMaxChainId(ctx: EContext): Long?

    fun getBlockchainRid(ctx: EContext): BlockchainRid?
    fun insertBlock(ctx: EContext, height: Long): Long
    fun insertTransaction(ctx: BlockEContext, tx: Transaction): Long
    fun finalizeBlock(ctx: BlockEContext, header: BlockHeader)

    fun commitBlock(ctx: BlockEContext, w: BlockWitness)
    fun getBlockHeight(ctx: EContext, blockRID: ByteArray, chainId: Long): Long?
    fun getBlockRID(ctx: EContext, height: Long): ByteArray?
    fun getBlockHeader(ctx: EContext, blockRID: ByteArray): ByteArray
    fun getBlockTransactions(ctx: EContext, blockRID: ByteArray, hashesOnly: Boolean): List<TxDetail>
    fun getWitnessData(ctx: EContext, blockRID: ByteArray): ByteArray
    fun getLastBlockHeight(ctx: EContext): Long
    fun getLastBlockRid(ctx: EContext, chainId: Long): ByteArray?
    fun getBlockHeightInfo(ctx: EContext, bcRid: BlockchainRid): Pair<Long, ByteArray>?
    fun getLastBlockTimestamp(ctx: EContext): Long
    fun getTxRIDsAtHeight(ctx: EContext, height: Long): Array<ByteArray>
    fun getBlockInfo(ctx: EContext, txRID: ByteArray): BlockInfo
    fun getTxHash(ctx: EContext, txRID: ByteArray): ByteArray
    fun getBlockTxRIDs(ctx: EContext, blockIid: Long): List<ByteArray>
    fun getBlockTxHashes(ctx: EContext, blokcIid: Long): List<ByteArray>
    fun getTxBytes(ctx: EContext, txRID: ByteArray): ByteArray?
    fun isTransactionConfirmed(ctx: EContext, txRID: ByteArray): Boolean
    fun getBlock(ctx: EContext, blockRID: ByteArray): BlockInfoExt?
    fun getBlocks(ctx: EContext, blockTime: Long, limit: Int): List<BlockInfoExt>
    fun getTransactionInfo(ctx: EContext, txRID: ByteArray): TransactionInfoExt?
    fun getTransactionsInfo(ctx: EContext, beforeTime: Long, limit: Int): List<TransactionInfoExt>

    // Blockchain configurations
    fun findConfigurationHeightForBlock(ctx: EContext, height: Long): Long?

    fun getConfigurationData(ctx: EContext, height: Long): ByteArray?
    fun addConfigurationData(ctx: EContext, height: Long, data: ByteArray)

    fun getPeerInfoCollection(ctx: AppContext): Array<PeerInfo>
    fun findPeerInfo(ctx: AppContext, host: String?, port: Int?, pubKeyPattern: String?): Array<PeerInfo>
    fun addPeerInfo(ctx: AppContext, peerInfo: PeerInfo): Boolean
    fun addPeerInfo(ctx: AppContext, host: String, port: Int, pubKey: String, timestamp: Instant? = null): Boolean
    fun updatePeerInfo(ctx: AppContext, host: String, port: Int, pubKey: String, timestamp: Instant? = null): Boolean
    fun removePeerInfo(ctx: AppContext, pubKey: String): Array<PeerInfo>

    // Extra nodes to sync from
    fun getBlockchainReplicaCollection(ctx: AppContext): Map<BlockchainRid, List<XPeerID>>
    fun existsBlockchainReplica(ctx: AppContext, brid: String, pubkey: String): Boolean
    fun addBlockchainReplica(ctx: AppContext, brid: String, pubKey: String): Boolean
    fun removeBlockchainReplica(ctx: AppContext, brid: String?, pubKey: String): Set<BlockchainRid>

    //Avoid potential chain split
    fun setMustSyncUntil(ctx: AppContext, blockchainRID: BlockchainRid, height: Long): Boolean
    fun getMustSyncUntil(ctx: AppContext): Map<Long, Long>
    fun getChainIds(ctx: AppContext): Map<BlockchainRid, Long>

    companion object {
        fun of(ctx: AppContext): DatabaseAccess {
            return ctx.getInterface(DatabaseAccess::class.java)
                    ?: throw ProgrammerMistake("DatabaseAccess not accessible through EContext")
        }
    }
}
