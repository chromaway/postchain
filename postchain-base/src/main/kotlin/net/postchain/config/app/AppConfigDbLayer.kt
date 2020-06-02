// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.config.app

import net.postchain.base.BlockchainRid
import net.postchain.base.PeerInfo
import net.postchain.base.data.SQLDatabaseAccess.Companion.TABLE_PEERINFOS_FIELD_HOST
import net.postchain.base.data.SQLDatabaseAccess.Companion.TABLE_PEERINFOS_FIELD_PORT
import net.postchain.base.data.SQLDatabaseAccess.Companion.TABLE_PEERINFOS_FIELD_PUBKEY
import net.postchain.base.data.SQLDatabaseAccess.Companion.TABLE_PEERINFOS_FIELD_TIMESTAMP
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.MapListHandler
import org.apache.commons.dbutils.handlers.ScalarHandler
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant

@Deprecated("POS-128")
class AppConfigDbLayer(
        private val appConfig: AppConfig,
        private val connection: Connection
) {

    init {
        createSchemaIfNotExists(connection)
        createTablesIfNotExists(appConfig, connection)
    }

    @Deprecated("POS-128")
    val TABLE_PEERINFOS = "@Deprecated(\"POS-128\")"

    @Deprecated("POS-128")
    fun getPeerInfoCollection(): Array<PeerInfo> {
        return findPeerInfo(null, null, null)
    }

    @Deprecated("POS-128")
    fun findPeerInfo(host: String?, port: Int?, pubKeyPattern: String?): Array<PeerInfo> {
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
            "SELECT * FROM $TABLE_PEERINFOS"
        } else {
            conditions.joinToString(
                    separator = " AND ",
                    prefix = "SELECT * FROM $TABLE_PEERINFOS WHERE "
            )
        }

        // Running the query
        val rawPeerInfos: MutableList<MutableMap<String, Any>> = QueryRunner().query(
                connection, query, MapListHandler())

        return rawPeerInfos.map {
            PeerInfo(
                    it[TABLE_PEERINFOS_FIELD_HOST] as String,
                    it[TABLE_PEERINFOS_FIELD_PORT] as Int,
                    (it[TABLE_PEERINFOS_FIELD_PUBKEY] as String).hexStringToByteArray(),
                    (it[TABLE_PEERINFOS_FIELD_TIMESTAMP] as? Timestamp)?.toInstant() ?: Instant.EPOCH
            )
        }.toTypedArray()
    }

    @Deprecated("POS-128")
    fun addPeerInfo(peerInfo: PeerInfo): Boolean {
        return addPeerInfo(peerInfo.host, peerInfo.port, peerInfo.pubKey.toHex())
    }

    @Deprecated("POS-128")
    fun addPeerInfo(host: String, port: Int, pubKey: String, timestamp: Instant? = null): Boolean {
        val time = getTimestamp(timestamp)
        return pubKey == QueryRunner().insert(
                connection,
                "INSERT INTO $TABLE_PEERINFOS " +
                        "($TABLE_PEERINFOS_FIELD_HOST, $TABLE_PEERINFOS_FIELD_PORT, $TABLE_PEERINFOS_FIELD_PUBKEY, $TABLE_PEERINFOS_FIELD_TIMESTAMP) " +
                        "VALUES (?, ?, ?, ?) " +
                        "RETURNING $TABLE_PEERINFOS_FIELD_PUBKEY",
                ScalarHandler<String>(), host, port, pubKey, time)
    }

    @Deprecated("POS-128")
    fun updatePeerInfo(host: String, port: Int, pubKey: String, timestamp: Instant? = null): Boolean {
        val time = getTimestamp(timestamp)
        val updated = QueryRunner().update(
                connection,
                "UPDATE $TABLE_PEERINFOS " +
                        "SET $TABLE_PEERINFOS_FIELD_HOST = ?, $TABLE_PEERINFOS_FIELD_PORT = ?, $TABLE_PEERINFOS_FIELD_TIMESTAMP = ? " +
                        "WHERE $TABLE_PEERINFOS_FIELD_PUBKEY = ?",
                ScalarHandler<Int>(), host, port, time, pubKey)

        return (updated >= 1)
    }

    @Deprecated("POS-128")
    fun removePeerInfo(pubKey: String): Array<PeerInfo> {
        val result = mutableListOf<PeerInfo>()

        val queryRunner = QueryRunner()
        val peerInfos = findPeerInfo(null, null, pubKey)
        peerInfos.forEach { peeInfo ->
            val deleted = queryRunner.update(
                    connection,
                    "DELETE FROM $TABLE_PEERINFOS WHERE $TABLE_PEERINFOS_FIELD_PUBKEY = '${peeInfo.pubKey.toHex()}'")

            if (deleted == 1) {
                result.add(peeInfo)
            }
        }

        return result.toTypedArray()
    }

    @Deprecated("POS-128")
    fun getBlockchainRid(chainIid: Long): BlockchainRid? {
        val queryRunner = QueryRunner()
        val data = queryRunner.query(connection, "SELECT blockchain_rid FROM blockchains WHERE chain_iid= ?",
                ScalarHandler<ByteArray?>(), chainIid)
        return if (data == null) null else BlockchainRid(data)
    }

    @Deprecated("POS-128")
    private fun getTimestamp(time: Instant? = null): Timestamp {
        return if (time == null) {
            Timestamp(Instant.now().toEpochMilli())
        } else {
            Timestamp(time.toEpochMilli())
        }
    }

    @Deprecated("POS-128")
    private fun createSchemaIfNotExists(connection: Connection) {
        QueryRunner().update(connection, "CREATE SCHEMA IF NOT EXISTS ${appConfig.databaseSchema}")
        connection.commit()
    }

    @Deprecated("POS-128")
    private fun createTablesIfNotExists(appConfig: AppConfig, connection: Connection) {
//        val sqlCommands = SQLCommandsFactory.getSQLCommands(appConfig.databaseDriverclass)
//        SQLDatabaseAccess(sqlCommands).initialize(connection, expectedDbVersion = 1) // TODO: [et]: Extract version
//        connection.commit()
    }
}