package net.postchain.base.data

import mu.KLogging
import net.postchain.base.BaseBlockHeader
import net.postchain.base.BlockchainRid
import net.postchain.base.PeerInfo
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.core.*
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.*
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant


abstract class SQLDatabaseAccess : DatabaseAccess {

    protected fun tableMeta(): String = "meta"
    protected fun tableBlockchains(): String = "blockchains"
    protected fun tablePeerinfos(): String = "peerinfos"
    protected fun tableConfigurations(ctx: EContext): String = chainTableName(ctx, "configurations")
    protected fun tableTransactions(ctx: EContext): String = chainTableName(ctx, "transactions")
    protected fun tableBlocks(ctx: EContext): String = chainTableName(ctx, "blocks")
    fun tableGtxModuleVersion(ctx: EContext): String = chainTableName(ctx, "gtx_module_version")
    fun chainTableName(ctx: EContext, table: String): String {
        return table
//        return "c${ctx.chainID}.$table" // Will be uncommented later
    }

    protected abstract fun cmdCreateTableMeta(): String
    protected abstract fun cmdCreateTableBlockchains(): String
    protected abstract fun cmdCreateTablePeerInfos(): String
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
        queryRunner.update(ctx.conn, cmdInsertBlocks(ctx), ctx.chainID, height)

        val sql = "SELECT block_iid FROM ${tableBlocks(ctx)}" +
                " WHERE chain_iid = ? and block_height = ?"
        return queryRunner.query(ctx.conn, sql, longRes, ctx.chainID, height)
    }

    override fun insertTransaction(ctx: BlockEContext, tx: Transaction): Long {
        queryRunner.update(ctx.conn, cmdInsertTransactions(ctx), ctx.chainID, tx.getRID(), tx.getRawData(), tx.getHash(), ctx.blockIID)

        val sql = "SELECT tx_iid FROM ${tableTransactions(ctx)}" +
                " WHERE chain_iid= ? and tx_rid = ?"
        return queryRunner.query(ctx.conn, sql, longRes, ctx.chainID, tx.getRID())
    }

    override fun finalizeBlock(ctx: BlockEContext, header: BlockHeader) {
        val sql = "UPDATE ${tableBlocks(ctx)} SET block_rid = ?, block_header_data = ?, timestamp = ?" +
                " WHERE chain_iid= ? AND block_iid = ?"

        queryRunner.update(ctx.conn, sql,
                header.blockRID, header.rawData, (header as BaseBlockHeader).timestamp, ctx.chainID, ctx.blockIID
        )
    }

    override fun commitBlock(ctx: BlockEContext, w: BlockWitness) {
        val sql = "UPDATE ${tableBlocks(ctx)} SET block_witness = ? WHERE block_iid=?"
        queryRunner.update(ctx.conn, sql, w.getRawData(), ctx.blockIID)
    }

    override fun getBlockHeight(ctx: EContext, blockRID: ByteArray, chainId: Long): Long? {
        val sql = "SELECT block_height FROM ${tableBlocks(ctx)} where chain_iid= ? and block_rid = ?"
        return queryRunner.query(ctx.conn, sql, nullableLongRes, chainId, blockRID)
    }

    // The combination of CHAIN_ID and BLOCK_HEIGHT is unique
    override fun getBlockRID(ctx: EContext, height: Long): ByteArray? {
        val sql = "SELECT block_rid FROM ${tableBlocks(ctx)} WHERE chain_iid= ? AND block_height = ?"
        return queryRunner.query(ctx.conn, sql, nullableByteArrayRes, ctx.chainID, height)
    }

    override fun getBlockHeader(ctx: EContext, blockRID: ByteArray): ByteArray {
        val sql = "SELECT block_header_data FROM ${tableBlocks(ctx)} where chain_iid= ? and block_rid = ?"
        return queryRunner.query(ctx.conn, sql, byteArrayRes, ctx.chainID, blockRID)
    }

    override fun getBlockTransactions(ctx: EContext, blockRID: ByteArray, hashesOnly: Boolean): List<TxDetail> {
        val sql = """
            SELECT tx_rid, tx_hash${if (hashesOnly) "" else ", tx_data"}
            FROM ${tableTransactions(ctx)} t
            JOIN ${tableBlocks(ctx)} b ON t.block_iid=b.block_iid
            WHERE b.block_rid=? AND b.chain_iid=?
            ORDER BY tx_iid
        """.trimIndent()

        val txs = queryRunner.query(ctx.conn, sql, mapListHandler, blockRID, ctx.chainID)

        return txs.map { tx ->
            TxDetail(
                    tx["tx_rid"] as ByteArray,
                    tx["tx_hash"] as ByteArray,
                    if (hashesOnly) null else (tx["tx_data"] as ByteArray)
            )
        }
    }

    override fun getWitnessData(ctx: EContext, blockRID: ByteArray): ByteArray {
        val sql = "SELECT block_witness FROM ${tableBlocks(ctx)} WHERE chain_iid= ? AND block_rid = ?"
        return queryRunner.query(ctx.conn, sql, byteArrayRes, ctx.chainID, blockRID)
    }

    override fun getLastBlockHeight(ctx: EContext): Long {
        val sql = "SELECT block_height FROM ${tableBlocks(ctx)} WHERE chain_iid= ? ORDER BY block_height DESC LIMIT 1"
        return queryRunner.query(ctx.conn, sql, longRes, ctx.chainID) ?: -1L
    }

    override fun getLastBlockRid(ctx: EContext, chainId: Long): ByteArray? {
        val sql = "SELECT block_rid FROM ${tableBlocks(ctx)} WHERE chain_iid= ? ORDER BY block_height DESC LIMIT 1"
        return queryRunner.query(ctx.conn, sql, nullableByteArrayRes, chainId)
    }

    override fun getBlockHeightInfo(ctx: EContext, bcRid: BlockchainRid): Pair<Long, ByteArray>? {
        val res = queryRunner.query(ctx.conn, """
                    SELECT b.block_height, b.block_rid
                         FROM ${tableBlocks(ctx)} b
                         JOIN ${tableBlockchains()} bc ON bc.chain_iid= b.chain_iid
                         WHERE bc.blockchain_rid = ?
                         ORDER BY b.block_height DESC LIMIT 1
                         """, mapListHandler, bcRid.data)

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
        val sql = "SELECT timestamp FROM ${tableBlocks(ctx)} WHERE chain_iid= ? ORDER BY timestamp DESC LIMIT 1"
        return queryRunner.query(ctx.conn, sql, longRes, ctx.chainID) ?: -1L
    }

    override fun getTxRIDsAtHeight(ctx: EContext, height: Long): Array<ByteArray> {
        val sql = "SELECT tx_rid FROM" +
                " ${tableTransactions(ctx)} t" +
                " INNER JOIN ${tableBlocks(ctx)} b ON t.block_iid=b.block_iid" +
                " WHERE b.block_height=? and b.chain_iid=?"
        return queryRunner.query(ctx.conn, sql, ColumnListHandler<ByteArray>(), height, ctx.chainID).toTypedArray()
    }

    override fun getBlockInfo(ctx: EContext, txRID: ByteArray): DatabaseAccess.BlockInfo {
        val sql = """
            SELECT b.block_iid, b.block_header_data, b.block_witness
                    FROM ${tableBlocks(ctx)} b
                    JOIN ${tableTransactions(ctx)} t ON b.block_iid=t.block_iid
                    WHERE t.chain_iid=? and t.tx_rid=?
        """.trimIndent()
        val block = queryRunner.query(ctx.conn, sql, mapListHandler, ctx.chainID, txRID)!!
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
                    FROM ${tableBlocks(ctx)} as b JOIN ${tableTransactions(ctx)} as t ON (t.block_iid = b.block_iid) 
                    WHERE b.chain_iid=? and t.tx_rid = ?
                    ORDER BY b.block_height DESC 
                    LIMIT 1;
        """.trimIndent()
        val txInfos = queryRunner.query(ctx.conn, sql, mapListHandler, ctx.chainID, txRID)
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
                    FROM ${tableBlocks(ctx)} as b JOIN ${tableTransactions(ctx)} as t ON (t.block_iid = b.block_iid) 
                    WHERE b.chain_iid=? and b.timestamp < ? 
                    ORDER BY b.block_height DESC 
                    LIMIT ?;
        """.trimIndent()
        val transactions = queryRunner.query(ctx.conn, sql, mapListHandler, ctx.chainID, beforeTime, limit)
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
        val sql = "SELECT tx_hash FROM ${tableTransactions(ctx)} WHERE tx_rid = ? and chain_iid=?"
        return queryRunner.query(ctx.conn, sql, byteArrayRes, txRID, ctx.chainID)
    }


    override fun getBlockTxRIDs(ctx: EContext, blockIid: Long): List<ByteArray> {
        return queryRunner.query(ctx.conn,
                "SELECT tx_rid FROM " +
                        "${tableTransactions(ctx)} t " +
                        "where t.block_iid=? order by tx_iid",
                ColumnListHandler<ByteArray>(), blockIid)!!
    }

    override fun getBlockTxHashes(ctx: EContext, blockIid: Long): List<ByteArray> {
        return queryRunner.query(ctx.conn,
                "SELECT tx_hash FROM " +
                        "${tableTransactions(ctx)} t " +
                        "where t.block_iid=? order by tx_iid",
                ColumnListHandler<ByteArray>(), blockIid)!!
    }

    override fun getTxBytes(ctx: EContext, txRID: ByteArray): ByteArray? {
        return queryRunner.query(ctx.conn, "SELECT tx_data FROM ${tableTransactions(ctx)} WHERE chain_iid=? AND tx_rid=?",
                nullableByteArrayRes, ctx.chainID, txRID)
    }

    override fun isTransactionConfirmed(ctx: EContext, txRID: ByteArray): Boolean {
        val res = queryRunner.query(ctx.conn,
                """
                        SELECT 1 FROM ${tableTransactions(ctx)} t
                        WHERE t.chain_iid=? AND t.tx_rid=?
                        """, nullableIntRes, ctx.chainID, txRID)
        return (res != null)
    }

    override fun getBlockchainRid(ctx: EContext): BlockchainRid? {
        val sql = "SELECT blockchain_rid FROM ${tableBlockchains()} WHERE chain_iid= ?"
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
        }
    }

    // TODO: [POS-128]: Remove it as soon as prefixes `c0.` are used
    private fun createIfNotExist(ctx: EContext, table: String, createCmd: String) {
        if (!tableExists(ctx.conn, table)) {
            queryRunner.update(ctx.conn, createCmd)
        }
    }

    override fun initializeBlockchain(ctx: EContext, blockchainRid: BlockchainRid) {
        // TODO: [POS-128]: Temporal solution
        val initialized = getBlockchainRid(ctx) != null

        createIfNotExist(ctx, tableBlocks(ctx), cmdCreateTableBlocks(ctx))
        createIfNotExist(ctx, tableTransactions(ctx), cmdCreateTableTransactions(ctx))
        createIfNotExist(ctx, tableConfigurations(ctx), cmdCreateTableConfigurations(ctx))

        // TODO: [POS-128]: Temporal solution
        val indexCreated = tableExists(ctx.conn, tableTransactions(ctx))
        if (!indexCreated) {
            queryRunner.update(ctx.conn,
                    """CREATE INDEX transactions_block_iid_idx ON ${tableTransactions(ctx)}(block_iid)""")
        }

        // TODO: [POS-128]: Temporal solution
        val indexCreated2 = tableExists(ctx.conn, tableBlocks(ctx))
        if (!indexCreated2) {
            queryRunner.update(ctx.conn,
                    """CREATE INDEX blocks_chain_iid_timestamp ON ${tableBlocks(ctx)}(chain_iid, timestamp)""")
        }

        /*
        if (!tableExists(configurations)) {
            queryRunner.update(connection, """CREATE INDEX configurations_chain_iid_to_height ON configurations(chain_iid, height)""")
        }
         */

        if (!initialized) {
            // Inserting chainId -> blockchainRid
            val sql = "INSERT INTO ${tableBlockchains()} (chain_iid, blockchain_rid) values (?, ?)"
            queryRunner.update(ctx.conn, sql, ctx.chainID, blockchainRid.data)
        }
    }

    override fun getChainId(ctx: EContext, blockchainRID: BlockchainRid): Long? {
        val sql = "SELECT chain_iid FROM ${tableBlockchains()} WHERE blockchain_rid=?"
        return queryRunner.query(ctx.conn, sql, nullableLongRes, blockchainRID.data)
    }

    override fun getMaxChainId(ctx: EContext): Long? {
        val sql = "SELECT MAX(chain_iid) FROM blockchains"
        return queryRunner.query(ctx.conn, sql, nullableLongRes)
    }

    override fun getBlock(ctx: EContext, blockRID: ByteArray): DatabaseAccess.BlockInfoExt? {
        val sql = "SELECT block_rid, block_height, block_header_data, block_witness, timestamp " +
                "FROM ${tableBlocks(ctx)} WHERE  chain_iid=? and block_rid = ? " +
                "LIMIT 1"
        val blockInfos = queryRunner.query(ctx.conn, sql, mapListHandler, ctx.chainID, blockRID)
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
        val sql = "SELECT block_rid, block_height, block_header_data, block_witness, timestamp " +
                "FROM ${tableBlocks(ctx)} WHERE chain_iid=? and timestamp < ? " +
                "ORDER BY timestamp DESC " +
                "LIMIT ?"
        val blocksInfo = queryRunner.query(ctx.conn, sql, mapListHandler, ctx.chainID, blockTime, limit)

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
        val sql = "SELECT height FROM ${tableConfigurations(ctx)} WHERE chain_iid= ? AND height <= ? " +
                "ORDER BY height DESC LIMIT 1"
        return queryRunner.query(ctx.conn, sql, nullableLongRes, ctx.chainID, height)
    }

    override fun getConfigurationData(ctx: EContext, height: Long): ByteArray? {
        val sql = "SELECT configuration_data FROM ${tableConfigurations(ctx)} WHERE chain_iid= ? AND height = ?"
        return queryRunner.query(ctx.conn, sql, nullableByteArrayRes, ctx.chainID, height)
    }

    override fun addConfigurationData(ctx: EContext, height: Long, data: ByteArray) {
        queryRunner.update(ctx.conn,
                cmdInsertConfiguration(ctx),
                ctx.chainID, height, data)
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
        val sql = "INSERT INTO ${tablePeerinfos()} " +
                "($TABLE_PEERINFOS_FIELD_HOST, $TABLE_PEERINFOS_FIELD_PORT, $TABLE_PEERINFOS_FIELD_PUBKEY, $TABLE_PEERINFOS_FIELD_TIMESTAMP) " +
                "VALUES (?, ?, ?, ?) " +
                "RETURNING $TABLE_PEERINFOS_FIELD_PUBKEY"
        return pubKey == queryRunner.insert(
                ctx.conn, sql, ScalarHandler<String>(), host, port, pubKey, time)
    }

    override fun updatePeerInfo(ctx: AppContext, host: String, port: Int, pubKey: String, timestamp: Instant?): Boolean {
        val time = SqlUtils.toTimestamp(timestamp)
        val sql = "UPDATE ${tablePeerinfos()} " +
                "SET $TABLE_PEERINFOS_FIELD_HOST = ?, $TABLE_PEERINFOS_FIELD_PORT = ?, $TABLE_PEERINFOS_FIELD_TIMESTAMP = ? " +
                "WHERE $TABLE_PEERINFOS_FIELD_PUBKEY = ?"
        val updated = queryRunner.update(
                ctx.conn, sql, ScalarHandler<Int>(), host, port, time, pubKey)

        return (updated >= 1)
    }

    override fun removePeerInfo(ctx: AppContext, pubKey: String): Array<PeerInfo> {
        val result = mutableListOf<PeerInfo>()

        val peerInfos = findPeerInfo(ctx, null, null, pubKey)
        peerInfos.forEach { peeInfo ->
            val sql = "DELETE FROM ${tablePeerinfos()}" +
                    " WHERE $TABLE_PEERINFOS_FIELD_PUBKEY = '${peeInfo.pubKey.toHex()}'"
            val deleted = queryRunner.update(ctx.conn, sql)

            if (deleted == 1) {
                result.add(peeInfo)
            }
        }

        return result.toTypedArray()
    }

    fun tableExists(connection: Connection, tableName_: String): Boolean {
        val types: Array<String> = arrayOf("TABLE")

        val tableName = if (connection.metaData.storesUpperCaseIdentifiers()) {
            tableName_.toUpperCase()
        } else {
            tableName_
        }

        val rs = connection.metaData.getTables(null, null, tableName, types)
        while (rs.next()) {
            // avoid wildcard '_' in SQL. Eg: if you pass "employee_salary" that should return something employeesalary which we don't expect
            if (rs.getString(2).toLowerCase() == connection.schema.toLowerCase()
                    && rs.getString(3).toLowerCase() == tableName.toLowerCase()) {
                return true
            }
        }
        return false
    }

}
