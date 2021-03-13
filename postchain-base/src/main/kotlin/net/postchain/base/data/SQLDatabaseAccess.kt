package net.postchain.base.data

import mu.KLogging
import net.postchain.base.BaseBlockHeader
import net.postchain.base.BlockchainRid
import net.postchain.base.PeerInfo
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.core.*
import net.postchain.network.x.XPeerID
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.*
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant


abstract class SQLDatabaseAccess : DatabaseAccess {

    protected fun tableMeta(): String = "meta"
    protected fun tableBlockchains(): String = "blockchains"
    protected fun tablePeerinfos(): String = "peerinfos"
    protected fun tableBlockchainReplicas(): String = "blockchain_replicas"
    protected fun tableMustSyncUntil(): String = "must_sync_until"
    protected fun tableConfigurations(ctx: EContext): String = tableName(ctx, "configurations")
    protected fun tableTransactions(ctx: EContext): String = tableName(ctx, "transactions")
    protected fun tableBlocks(ctx: EContext): String = tableName(ctx, "blocks")
    protected fun tableBlocks(chainId: Long): String = tableName(chainId, "blocks")
    fun tableGtxModuleVersion(ctx: EContext): String = tableName(ctx, "gtx_module_version")

    override fun tableName(ctx: EContext, table: String): String {
        return tableName(ctx.chainID, table)
    }

    fun tableName(chainId: Long, table: String): String {
        return "\"c${chainId}.$table\""
    }

    protected abstract fun cmdCreateTableMeta(): String
    protected abstract fun cmdCreateTableBlockchains(): String
    protected abstract fun cmdCreateTablePeerInfos(): String
    protected abstract fun cmdCreateTableBlockchainReplicas(): String
    protected abstract fun cmdCreateTableMustSyncUntil(): String
    protected abstract fun cmdCreateTableConfigurations(ctx: EContext): String
    protected abstract fun cmdCreateTableTransactions(ctx: EContext): String
    protected abstract fun cmdCreateTableBlocks(ctx: EContext): String
    protected abstract fun cmdInsertBlocks(ctx: EContext): String
    protected abstract fun cmdInsertTransactions(ctx: EContext): String
    protected abstract fun cmdInsertConfiguration(ctx: EContext): String
    abstract fun cmdCreateTableGtxModuleVersion(ctx: EContext): String

    var queryRunner = QueryRunner()
    private val intRes = ScalarHandler<Int>()
    val longRes = ScalarHandler<Long>()
    private val signatureRes = BeanListHandler<Signature>(Signature::class.java)
    private val nullableByteArrayRes = ScalarHandler<ByteArray?>()
    private val nullableIntRes = ScalarHandler<Int?>()
    private val nullableLongRes = ScalarHandler<Long?>()
    private val byteArrayRes = ScalarHandler<ByteArray>()
    private val blockDataRes = BeanHandler<BlockData>(BlockData::class.java)
    private val byteArrayListRes = ColumnListHandler<ByteArray>()
    private val mapListHandler = MapListHandler()
    private val stringRes = ScalarHandler<String>()

    companion object : KLogging() {
        const val TABLE_PEERINFOS_FIELD_HOST = "host"
        const val TABLE_PEERINFOS_FIELD_PORT = "port"
        const val TABLE_PEERINFOS_FIELD_PUBKEY = "pub_key"
        const val TABLE_PEERINFOS_FIELD_TIMESTAMP = "timestamp"

        const val TABLE_REPLICAS_FIELD_BRID = "blockchain_rid"
        const val TABLE_REPLICAS_FIELD_PUBKEY = "node"

        const val TABLE_SYNC_UNTIL_FIELD_CHAIN_IID = "chain_iid"
        const val TABLE_SYNC_UNTIL_FIELD_HEIGHT = "block_height"
    }

    override fun isSchemaExists(connection: Connection, schema: String): Boolean {
        val schemas = connection.metaData.schemas

        while (schemas.next()) {
            if (schemas.getString(1).toLowerCase() == schema.toLowerCase()) {
                return true
            }
        }

        return false
    }

    override fun insertBlock(ctx: EContext, height: Long): Long {
        queryRunner.update(ctx.conn, cmdInsertBlocks(ctx), height)

        val sql = "SELECT block_iid FROM ${tableBlocks(ctx)} WHERE block_height = ?"
        return queryRunner.query(ctx.conn, sql, longRes, height)
    }

    override fun insertTransaction(ctx: BlockEContext, tx: Transaction): Long {
        queryRunner.update(ctx.conn, cmdInsertTransactions(ctx), tx.getRID(), tx.getRawData(), tx.getHash(), ctx.blockIID)

        val sql = "SELECT tx_iid FROM ${tableTransactions(ctx)} WHERE tx_rid = ?"
        return queryRunner.query(ctx.conn, sql, longRes, tx.getRID())
    }

    override fun finalizeBlock(ctx: BlockEContext, header: BlockHeader) {
        val sql = "UPDATE ${tableBlocks(ctx)} SET block_rid = ?, block_header_data = ?, timestamp = ? WHERE block_iid = ?"
        queryRunner.update(ctx.conn, sql,
                header.blockRID, header.rawData, (header as BaseBlockHeader).timestamp, ctx.blockIID
        )
    }

    override fun commitBlock(ctx: BlockEContext, w: BlockWitness) {
        val sql = "UPDATE ${tableBlocks(ctx)} SET block_witness = ? WHERE block_iid = ?"
        queryRunner.update(ctx.conn, sql, w.getRawData(), ctx.blockIID)
    }

    override fun getBlockHeight(ctx: EContext, blockRID: ByteArray, chainId: Long): Long? {
        val sql = "SELECT block_height FROM ${tableBlocks(chainId)} WHERE block_rid = ?"
        return queryRunner.query(ctx.conn, sql, nullableLongRes, blockRID)
    }

    // The combination of CHAIN_ID and BLOCK_HEIGHT is unique
    override fun getBlockRID(ctx: EContext, height: Long): ByteArray? {
        val sql = "SELECT block_rid FROM ${tableBlocks(ctx)} WHERE block_height = ?"
        return queryRunner.query(ctx.conn, sql, nullableByteArrayRes, height)
    }

    override fun getBlockHeader(ctx: EContext, blockRID: ByteArray): ByteArray {
        val sql = "SELECT block_header_data FROM ${tableBlocks(ctx)} WHERE block_rid = ?"
        return queryRunner.query(ctx.conn, sql, byteArrayRes, blockRID)
    }

    override fun getBlockTransactions(ctx: EContext, blockRID: ByteArray, hashesOnly: Boolean): List<TxDetail> {
        val sql = """
            SELECT tx_rid, tx_hash${if (hashesOnly) "" else ", tx_data"}
            FROM ${tableTransactions(ctx)} t
            JOIN ${tableBlocks(ctx)} b ON t.block_iid=b.block_iid
            WHERE b.block_rid=? ORDER BY tx_iid
        """.trimIndent()

        val txs = queryRunner.query(ctx.conn, sql, mapListHandler, blockRID)

        return txs.map { tx ->
            TxDetail(
                    tx["tx_rid"] as ByteArray,
                    tx["tx_hash"] as ByteArray,
                    if (hashesOnly) null else (tx["tx_data"] as ByteArray)
            )
        }
    }

    override fun getWitnessData(ctx: EContext, blockRID: ByteArray): ByteArray {
        val sql = "SELECT block_witness FROM ${tableBlocks(ctx)} WHERE block_rid = ?"
        return queryRunner.query(ctx.conn, sql, byteArrayRes, blockRID)
    }

    override fun getLastBlockHeight(ctx: EContext): Long {
        val sql = "SELECT block_height FROM ${tableBlocks(ctx)} ORDER BY block_height DESC LIMIT 1"
        return queryRunner.query(ctx.conn, sql, longRes) ?: -1L
    }

    override fun getLastBlockRid(ctx: EContext, chainId: Long): ByteArray? {
        val sql = "SELECT block_rid FROM ${tableBlocks(chainId)} ORDER BY block_height DESC LIMIT 1"
        return queryRunner.query(ctx.conn, sql, nullableByteArrayRes)
    }

    override fun getBlockHeightInfo(ctx: EContext, bcRid: BlockchainRid): Pair<Long, ByteArray>? {
        val chainId = getChainId(ctx, bcRid) ?: return null

        val sql = "SELECT block_height, block_rid FROM ${tableBlocks(chainId)} ORDER BY block_height DESC LIMIT 1"
        val res = queryRunner.query(ctx.conn, sql, mapListHandler)

        return when (res.size) {
            0 -> null // This is allowed, it (usually) means we don't have any blocks yet
            1 -> {
                val height = res.first()["block_height"] as Long
                val blockRid = res.first()["block_rid"] as ByteArray
                Pair(height, blockRid)
            }
            else -> {
                throw ProgrammerMistake("Incorrect query getBlockHeightInfo got many lines (${res.size})")
            }
        }
    }

    override fun getLastBlockTimestamp(ctx: EContext): Long {
        val sql = "SELECT timestamp FROM ${tableBlocks(ctx)} ORDER BY block_iid DESC LIMIT 1"
        return queryRunner.query(ctx.conn, sql, longRes) ?: -1L
    }

    override fun getTxRIDsAtHeight(ctx: EContext, height: Long): Array<ByteArray> {
        val sql = "SELECT tx_rid" +
                " FROM ${tableTransactions(ctx)} t" +
                " INNER JOIN ${tableBlocks(ctx)} b ON t.block_iid=b.block_iid" +
                " WHERE b.block_height = ?"
        return queryRunner.query(ctx.conn, sql, ColumnListHandler<ByteArray>(), height).toTypedArray()
    }

    override fun getBlockInfo(ctx: EContext, txRID: ByteArray): DatabaseAccess.BlockInfo {
        val sql = """
            SELECT b.block_iid, b.block_header_data, b.block_witness
                    FROM ${tableBlocks(ctx)} b
                    JOIN ${tableTransactions(ctx)} t ON b.block_iid=t.block_iid
                    WHERE t.tx_rid = ?
        """.trimIndent()
        val block = queryRunner.query(ctx.conn, sql, mapListHandler, txRID)!!
        if (block.size < 1) throw UserMistake("Can't get confirmation proof for nonexistent tx")
        if (block.size > 1) throw ProgrammerMistake("Expected at most one hit")

        val blockIid = block.first()["block_iid"] as Long
        val blockHeader = block.first()["block_header_data"] as ByteArray
        val witness = block.first()["block_witness"] as ByteArray
        return DatabaseAccess.BlockInfo(blockIid, blockHeader, witness)
    }

    override fun getTransactionInfo(ctx: EContext, txRID: ByteArray): TransactionInfoExt? {
        val sql = """
            SELECT b.block_rid, b.block_height, b.block_header_data, b.block_witness, b.timestamp, t.tx_rid, t.tx_hash, t.tx_data 
                    FROM ${tableBlocks(ctx)} as b 
                    JOIN ${tableTransactions(ctx)} as t ON (t.block_iid = b.block_iid) 
                    WHERE t.tx_rid = ?
                    ORDER BY b.block_height DESC LIMIT 1;
        """.trimIndent()

        val txInfos = queryRunner.query(ctx.conn, sql, mapListHandler, txRID)
        if (txInfos.isEmpty()) return null
        val txInfo = txInfos.first()

        val blockRID = txInfo["block_rid"] as ByteArray
        val blockHeight = txInfo["block_height"] as Long
        val blockHeader = txInfo["block_header_data"] as ByteArray
        val blockWitness = txInfo["block_witness"] as ByteArray
        val blockTimestamp = txInfo["timestamp"] as Long
        val txRID = txInfo["tx_rid"] as ByteArray
        val txHash = txInfo["tx_hash"] as ByteArray
        val txData = txInfo["tx_data"] as ByteArray
        return TransactionInfoExt(
                blockRID, blockHeight, blockHeader, blockWitness, blockTimestamp, txRID, txHash, txData)
    }

    override fun getTransactionsInfo(ctx: EContext, beforeTime: Long, limit: Int): List<TransactionInfoExt> {
        val sql = """
            SELECT b.block_rid, b.block_height, b.block_header_data, b.block_witness, b.timestamp, t.tx_rid, t.tx_hash, t.tx_data 
                    FROM ${tableBlocks(ctx)} as b 
                    JOIN ${tableTransactions(ctx)} as t ON (t.block_iid = b.block_iid) 
                    WHERE b.timestamp < ? 
                    ORDER BY b.block_height DESC LIMIT ?;
        """.trimIndent()
        val transactions = queryRunner.query(ctx.conn, sql, mapListHandler, beforeTime, limit)
        return transactions.map { txInfo ->
            val blockRID = txInfo["block_rid"] as ByteArray
            val blockHeight = txInfo["block_height"] as Long
            val blockHeader = txInfo["block_header_data"] as ByteArray
            val blockWitness = txInfo["block_witness"] as ByteArray
            val blockTimestamp = txInfo["timestamp"] as Long
            val txRID = txInfo["tx_rid"] as ByteArray
            val txHash = txInfo["tx_hash"] as ByteArray
            val txData = txInfo["tx_data"] as ByteArray
            TransactionInfoExt(
                    blockRID, blockHeight, blockHeader, blockWitness, blockTimestamp, txRID, txHash, txData)
        }
    }

    override fun getTxHash(ctx: EContext, txRID: ByteArray): ByteArray {
        val sql = "SELECT tx_hash FROM ${tableTransactions(ctx)} WHERE tx_rid = ?"
        return queryRunner.query(ctx.conn, sql, byteArrayRes, txRID)
    }

    override fun getBlockTxRIDs(ctx: EContext, blockIid: Long): List<ByteArray> {
        val sql = "SELECT tx_rid FROM ${tableTransactions(ctx)} t WHERE t.block_iid = ? ORDER BY tx_iid"
        return queryRunner.query(ctx.conn, sql, ColumnListHandler(), blockIid)!!
    }

    override fun getBlockTxHashes(ctx: EContext, blockIid: Long): List<ByteArray> {
        val sql = "SELECT tx_hash FROM ${tableTransactions(ctx)} t WHERE t.block_iid = ? ORDER BY tx_iid"
        return queryRunner.query(ctx.conn, sql, ColumnListHandler(), blockIid)!!
    }

    override fun getTxBytes(ctx: EContext, txRID: ByteArray): ByteArray? {
        val sql = "SELECT tx_data FROM ${tableTransactions(ctx)} WHERE tx_rid=?"
        return queryRunner.query(ctx.conn, sql, nullableByteArrayRes, txRID)
    }

    override fun isTransactionConfirmed(ctx: EContext, txRID: ByteArray): Boolean {
        val sql = "SELECT 1 FROM ${tableTransactions(ctx)} t WHERE t.tx_rid = ?"
        val res = queryRunner.query(ctx.conn, sql, nullableIntRes, txRID)
        return (res != null)
    }

    override fun getBlockchainRid(ctx: EContext): BlockchainRid? {
        val sql = "SELECT blockchain_rid FROM ${tableBlockchains()} WHERE chain_iid = ?"
        val data = queryRunner.query(ctx.conn, sql, nullableByteArrayRes, ctx.chainID)
        return data?.let(::BlockchainRid)
    }

    override fun initializeApp(connection: Connection, expectedDbVersion: Int) {
        /**
         * "CREATE TABLE IF NOT EXISTS" is not good enough for the meta table
         * We need to know whether it exists or not in order to
         * make decisions on upgrade
         */
        if (tableExists(connection, tableMeta())) {
            // meta table already exists. Check the version
            val sql = "SELECT value FROM ${tableMeta()} WHERE key='version'"
            val version = queryRunner.query(connection, sql, ScalarHandler<String>()).toInt()
            if (version != expectedDbVersion) {
                throw UserMistake("Unexpected version '$version' in database. Expected '$expectedDbVersion'")
            }

        } else {
            logger.info("Meta table does not exist! Assume database does not exist and create it (version: $expectedDbVersion).")
            queryRunner.update(connection, cmdCreateTableMeta())
            val sql = "INSERT INTO ${tableMeta()} (key, value) values ('version', ?)"
            queryRunner.update(connection, sql, expectedDbVersion)

            // Don't use "CREATE TABLE IF NOT EXISTS" because if they do exist
            // we must throw an error. If these tables exists but meta did not exist,
            // there is some serious problem that needs manual work
            queryRunner.update(connection, cmdCreateTableBlockchains())
            queryRunner.update(connection, cmdCreateTablePeerInfos())
            queryRunner.update(connection, cmdCreateTableBlockchainReplicas())
            queryRunner.update(connection, cmdCreateTableMustSyncUntil())
        }
    }

    override fun initializeBlockchain(ctx: EContext, blockchainRid: BlockchainRid) {
        val initialized = getBlockchainRid(ctx) != null

        queryRunner.update(ctx.conn, cmdCreateTableBlocks(ctx))
        queryRunner.update(ctx.conn, cmdCreateTableTransactions(ctx))
        queryRunner.update(ctx.conn, cmdCreateTableConfigurations(ctx))

        val txIndex = "CREATE INDEX IF NOT EXISTS ${tableName(ctx, "transactions_block_iid_idx")} " +
                "ON ${tableTransactions(ctx)}(block_iid)"
        queryRunner.update(ctx.conn, txIndex)

        val blockIndex = "CREATE INDEX IF NOT EXISTS ${tableName(ctx, "blocks_timestamp_idx")} " +
                "ON ${tableBlocks(ctx)}(timestamp)"
        queryRunner.update(ctx.conn, blockIndex)

        if (!initialized) {
            // Inserting chainId -> blockchainRid
            val sql = "INSERT INTO ${tableBlockchains()} (chain_iid, blockchain_rid) values (?, ?)"
            queryRunner.update(ctx.conn, sql, ctx.chainID, blockchainRid.data)
        }
    }

    override fun getChainId(ctx: EContext, blockchainRid: BlockchainRid): Long? {
        val sql = "SELECT chain_iid FROM ${tableBlockchains()} WHERE blockchain_rid = ?"
        return queryRunner.query(ctx.conn, sql, nullableLongRes, blockchainRid.data)
    }

    override fun getMaxChainId(ctx: EContext): Long? {
        val sql = "SELECT MAX(chain_iid) FROM ${tableBlockchains()}"
        return queryRunner.query(ctx.conn, sql, nullableLongRes)
    }

    override fun getBlock(ctx: EContext, blockRID: ByteArray): DatabaseAccess.BlockInfoExt? {
        val sql = """
            SELECT block_rid, block_height, block_header_data, block_witness, timestamp 
            FROM ${tableBlocks(ctx)} 
            WHERE block_rid = ? 
            LIMIT 1
        """.trimIndent()

        val blockInfos = queryRunner.query(ctx.conn, sql, mapListHandler, blockRID)
        if (blockInfos.isEmpty()) return null
        val blockInfo = blockInfos.first()

        val blockRid = blockInfo["block_rid"] as ByteArray
        val blockHeight = blockInfo["block_height"] as Long
        val blockHeader = blockInfo["block_header_data"] as ByteArray
        val blockWitness = blockInfo["block_witness"] as ByteArray
        val timestamp = blockInfo["timestamp"] as Long
        return DatabaseAccess.BlockInfoExt(blockRid, blockHeight, blockHeader, blockWitness, timestamp)
    }

    override fun getBlocks(ctx: EContext, blockTime: Long, limit: Int): List<DatabaseAccess.BlockInfoExt> {
        val sql = """
            SELECT block_rid, block_height, block_header_data, block_witness, timestamp 
            FROM ${tableBlocks(ctx)} 
            WHERE timestamp < ? 
            ORDER BY timestamp DESC LIMIT ?
        """.trimIndent()
        val blocksInfo = queryRunner.query(ctx.conn, sql, mapListHandler, blockTime, limit)

        return blocksInfo.map { blockInfo ->
            val blockRid = blockInfo["block_rid"] as ByteArray
            val heightOfBlock = blockInfo["block_height"] as Long
            val blockHeader = blockInfo["block_header_data"] as ByteArray
            val blockWitness = blockInfo["block_witness"] as ByteArray
            val timestamp = blockInfo["timestamp"] as Long
            DatabaseAccess.BlockInfoExt(
                    blockRid, heightOfBlock, blockHeader, blockWitness, timestamp)
        }
    }

    override fun findConfigurationHeightForBlock(ctx: EContext, height: Long): Long? {
        val sql = """
            SELECT height 
            FROM ${tableConfigurations(ctx)} 
            WHERE height <= ? 
            ORDER BY height DESC LIMIT 1
        """.trimIndent()
        return queryRunner.query(ctx.conn, sql, nullableLongRes, height)
    }

    override fun getConfigurationData(ctx: EContext, height: Long): ByteArray? {
        val sql = "SELECT configuration_data FROM ${tableConfigurations(ctx)} WHERE height = ?"
        return queryRunner.query(ctx.conn, sql, nullableByteArrayRes, height)
    }

    override fun addConfigurationData(ctx: EContext, height: Long, data: ByteArray) {
        queryRunner.update(ctx.conn, cmdInsertConfiguration(ctx), height, data)
    }

    override fun getPeerInfoCollection(ctx: AppContext): Array<PeerInfo> {
        return findPeerInfo(ctx, null, null, null)
    }

    override fun findPeerInfo(ctx: AppContext, host: String?, port: Int?, pubKeyPattern: String?): Array<PeerInfo> {
        // Collecting where's conditions
        val conditions = mutableListOf<String>()
        if (host != null) {
            conditions.add("$TABLE_PEERINFOS_FIELD_HOST = '$host'")
        }

        if (port != null) {
            conditions.add("$TABLE_PEERINFOS_FIELD_PORT = '$port'")
        }

        if (pubKeyPattern != null) {
            conditions.add("$TABLE_PEERINFOS_FIELD_PUBKEY ILIKE '%$pubKeyPattern%'")
        }

        // Building a query
        val query = if (conditions.isEmpty()) {
            "SELECT * FROM ${tablePeerinfos()}"
        } else {
            conditions.joinToString(
                    separator = " AND ",
                    prefix = "SELECT * FROM ${tablePeerinfos()} WHERE "
            )
        }

        // Running the query
        val rawPeerInfos: MutableList<MutableMap<String, Any>> = queryRunner.query(
                ctx.conn, query, MapListHandler())

        return rawPeerInfos.map {
            PeerInfo(
                    it[TABLE_PEERINFOS_FIELD_HOST] as String,
                    it[TABLE_PEERINFOS_FIELD_PORT] as Int,
                    (it[TABLE_PEERINFOS_FIELD_PUBKEY] as String).hexStringToByteArray(),
                    (it[TABLE_PEERINFOS_FIELD_TIMESTAMP] as? Timestamp)?.toInstant() ?: Instant.EPOCH
            )
        }.toTypedArray()
    }

    override fun addPeerInfo(ctx: AppContext, peerInfo: PeerInfo): Boolean {
        return addPeerInfo(ctx, peerInfo.host, peerInfo.port, peerInfo.pubKey.toHex())
    }

    override fun addPeerInfo(ctx: AppContext, host: String, port: Int, pubKey: String, timestamp: Instant?): Boolean {
        val time = SqlUtils.toTimestamp(timestamp)
        val sql = """
            INSERT INTO ${tablePeerinfos()} 
            ($TABLE_PEERINFOS_FIELD_HOST, $TABLE_PEERINFOS_FIELD_PORT, $TABLE_PEERINFOS_FIELD_PUBKEY, $TABLE_PEERINFOS_FIELD_TIMESTAMP) 
            VALUES (?, ?, ?, ?) RETURNING $TABLE_PEERINFOS_FIELD_PUBKEY
        """.trimIndent()
        return pubKey == queryRunner.insert(ctx.conn, sql, ScalarHandler<String>(), host, port, pubKey, time)
    }

    override fun updatePeerInfo(ctx: AppContext, host: String, port: Int, pubKey: String, timestamp: Instant?): Boolean {
        val time = SqlUtils.toTimestamp(timestamp)
        val sql = """
            UPDATE ${tablePeerinfos()} 
            SET $TABLE_PEERINFOS_FIELD_HOST = ?, $TABLE_PEERINFOS_FIELD_PORT = ?, $TABLE_PEERINFOS_FIELD_TIMESTAMP = ? 
            WHERE $TABLE_PEERINFOS_FIELD_PUBKEY = ?
        """.trimIndent()
        val updated = queryRunner.update(ctx.conn, sql, ScalarHandler<Int>(), host, port, time, pubKey)
        return (updated >= 1)
    }

    override fun removePeerInfo(ctx: AppContext, pubKey: String): Array<PeerInfo> {
        val result = mutableListOf<PeerInfo>()

        val peerInfos = findPeerInfo(ctx, null, null, pubKey)
        peerInfos.forEach { peeInfo ->
            val sql = """
                DELETE FROM ${tablePeerinfos()} 
                WHERE $TABLE_PEERINFOS_FIELD_PUBKEY = '${peeInfo.pubKey.toHex()}'
            """.trimIndent()
            val deleted = queryRunner.update(ctx.conn, sql)

            if (deleted == 1) {
                result.add(peeInfo)
            }
        }

        return result.toTypedArray()
    }

    override fun addBlockchainReplica(ctx: AppContext, brid: String, pubKey: String): Boolean {
        if (existsBlockchainReplica(ctx, brid, pubKey)) {
            return false
        }
        /*
        Due to reference integrity between tables peerInfos and BlockchainReplicas AND the fact that the pubkey string in peerInfos
        can hold both lower and upper characters (historically), we use the exact (case sensitive) value from the peerInfos table when
        adding the node as blockchain replica.
         */
        val sql = """
            INSERT INTO ${tableBlockchainReplicas()} 
            ($TABLE_REPLICAS_FIELD_BRID, $TABLE_REPLICAS_FIELD_PUBKEY) 
            VALUES (?, (SELECT $TABLE_PEERINFOS_FIELD_PUBKEY FROM ${tablePeerinfos()} WHERE lower($TABLE_PEERINFOS_FIELD_PUBKEY) = lower(?)))
        """.trimIndent()
        queryRunner.insert(ctx.conn, sql, ScalarHandler<String>(), brid, pubKey)
        return true
    }

    override fun getBlockchainReplicaCollection(ctx: AppContext): Map<BlockchainRid, List<XPeerID>> {

        val query = "SELECT * FROM ${tableBlockchainReplicas()}"

        val raw: MutableList<MutableMap<String, Any>> = queryRunner.query(
                ctx.conn, query, MapListHandler())

        /*
        Each MutableMap represents a row in the table.
        MutableList is thus a list of rows in the table.
         */
        return raw.groupBy(keySelector = { BlockchainRid((it[TABLE_REPLICAS_FIELD_BRID] as String).hexStringToByteArray()) },
                valueTransform = { XPeerID((it[TABLE_REPLICAS_FIELD_PUBKEY] as String).hexStringToByteArray()) })
    }

    override fun existsBlockchainReplica(ctx: AppContext, brid: String, pubkey: String): Boolean {
        val query = """
            SELECT count($TABLE_REPLICAS_FIELD_PUBKEY) 
            FROM ${tableBlockchainReplicas()}
            WHERE $TABLE_REPLICAS_FIELD_BRID = '$brid' AND
            lower($TABLE_REPLICAS_FIELD_PUBKEY) = lower('$pubkey') 
            """.trimIndent()

        return queryRunner.query(ctx.conn, query, ScalarHandler<Long>()) > 0
    }

    override fun removeBlockchainReplica(ctx: AppContext, brid: String?, pubkey: String): Set<BlockchainRid> {
        val delete = """DELETE FROM ${tableBlockchainReplicas()} 
                WHERE $TABLE_REPLICAS_FIELD_PUBKEY = ?"""
        val res = if (brid == null) {
            val sql = """
                $delete
                RETURNING *
            """.trimIndent()
            queryRunner.query(ctx.conn, sql, ColumnListHandler<String>(TABLE_REPLICAS_FIELD_BRID), pubkey)
        } else {
            val sql = """
                $delete
                AND $TABLE_REPLICAS_FIELD_BRID = ?
                RETURNING *
            """.trimIndent()
            queryRunner.query(ctx.conn, sql, ColumnListHandler<String>(TABLE_REPLICAS_FIELD_BRID), pubkey, brid)
        }
        return res.map { BlockchainRid.buildFromHex(it) }.toSet()
    }

    override fun setMustSyncUntil(ctx: AppContext, blockchainRID: BlockchainRid, height: Long): Boolean {
        // If given brid (chainID) already exist in table ( => CONFLICT), update table with the given height parameter.
        val sql = """
            INSERT INTO ${tableMustSyncUntil()} 
            ($TABLE_SYNC_UNTIL_FIELD_CHAIN_IID, $TABLE_SYNC_UNTIL_FIELD_HEIGHT) 
            VALUES ((SELECT chain_iid FROM ${tableBlockchains()} WHERE blockchain_rid = ?), ?) 
            ON CONFLICT ($TABLE_SYNC_UNTIL_FIELD_CHAIN_IID) DO UPDATE SET $TABLE_SYNC_UNTIL_FIELD_HEIGHT = ?
        """.trimIndent()
        queryRunner.insert(ctx.conn, sql, ScalarHandler<String>(), blockchainRID.data, height, height)
        return true
    }

    override fun getMustSyncUntil(ctx: AppContext): Map<Long, Long> {

        val query = "SELECT * FROM ${tableMustSyncUntil()}"
        val raw: MutableList<MutableMap<String, Any>> = queryRunner.query(
                ctx.conn, query, MapListHandler())
        /*
        Each MutableMap represents a row in the table.
        MutableList is thus a list of rows in the table.
         */
        return raw.map {
            it[TABLE_SYNC_UNTIL_FIELD_CHAIN_IID] as Long to
                    it[TABLE_SYNC_UNTIL_FIELD_HEIGHT] as Long
        }.toMap()
    }

    override fun getChainIds(ctx: AppContext): Map<BlockchainRid, Long> {
        val sql = "SELECT * FROM ${tableBlockchains()}"
        val raw: MutableList<MutableMap<String, Any>> = queryRunner.query(
                ctx.conn, sql, MapListHandler())

        return raw.map {
            BlockchainRid(it["blockchain_rid"] as ByteArray) to
                    it["chain_iid"] as Long
        }.toMap()
    }

    fun tableExists(connection: Connection, tableName: String): Boolean {
        val tableName0 = tableName.removeSurrounding("\"")
        val types: Array<String> = arrayOf("TABLE")
        val rs = connection.metaData.getTables(null, null, null, types)
        while (rs.next()) {
            // Avoid wildcard '_' in SQL. Eg: if you pass "employee_salary" that should return something
            // employeesalary which we don't expect
            if (rs.getString("TABLE_SCHEM").equals(connection.schema, true)
                    && rs.getString("TABLE_NAME").equals(tableName0, true)) {
                return true
            }
        }
        return false
    }

}
