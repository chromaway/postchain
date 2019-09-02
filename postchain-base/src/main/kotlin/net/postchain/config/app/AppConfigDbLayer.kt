package net.postchain.config.app

import net.postchain.base.PeerInfo
import net.postchain.base.data.SQLCommandsFactory
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.base.data.SQLDatabaseAccess.Companion.TABLE_PEERINFOS
import net.postchain.base.data.SQLDatabaseAccess.Companion.TABLE_PEERINFOS_FIELD_CREATED_AT
import net.postchain.base.data.SQLDatabaseAccess.Companion.TABLE_PEERINFOS_FIELD_HOST
import net.postchain.base.data.SQLDatabaseAccess.Companion.TABLE_PEERINFOS_FIELD_PORT
import net.postchain.base.data.SQLDatabaseAccess.Companion.TABLE_PEERINFOS_FIELD_PUBKEY
import net.postchain.base.data.SQLDatabaseAccess.Companion.TABLE_PEERINFOS_FIELD_UPDATED_AT
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.MapListHandler
import org.apache.commons.dbutils.handlers.ScalarHandler
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant

class AppConfigDbLayer(
        private val appConfig: AppConfig,
        private val connection: Connection
) {

    init {
        createSchemaIfNotExists(connection)
        createTablesIfNotExists(appConfig, connection)
    }

    fun getPeerInfoCollection(): Array<PeerInfo> {
        return findPeerInfo(null, null, null)
    }

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
                    Instant.ofEpochMilli((it[TABLE_PEERINFOS_FIELD_CREATED_AT] as Timestamp).time),
                    Instant.ofEpochMilli((it[TABLE_PEERINFOS_FIELD_UPDATED_AT] as Timestamp).time))
        }.toTypedArray()
    }

    fun addPeerInfo(host: String, port: Int, pubKey: String, createdAt: Instant? = null, updatedAt: Instant? = null): Boolean {
        val tsCreated = getTimestamp(createdAt)
        val tsUpdated = getTimestamp(updatedAt)
        return QueryRunner().insert(
                connection,
                "INSERT INTO $TABLE_PEERINFOS " +
                        "($TABLE_PEERINFOS_FIELD_HOST, $TABLE_PEERINFOS_FIELD_PORT, $TABLE_PEERINFOS_FIELD_PUBKEY, $TABLE_PEERINFOS_FIELD_CREATED_AT, $TABLE_PEERINFOS_FIELD_UPDATED_AT) " +
                        "VALUES (?, ?, ?, ?, ?) ",
                ScalarHandler<Boolean>(), host, port, pubKey, tsCreated, tsUpdated)
    }

    fun updatePeerInfo(host: String, port: Int, pubKey: String, updatedAt: Instant? = null): Boolean {
        val tsUpdated = getTimestamp(updatedAt)
        val updated = QueryRunner().update(
                connection,
                "UPDATE $TABLE_PEERINFOS " +
                        "SET $TABLE_PEERINFOS_FIELD_HOST = ?, $TABLE_PEERINFOS_FIELD_PORT = ?, $TABLE_PEERINFOS_FIELD_UPDATED_AT = ? " +
                        "WHERE $TABLE_PEERINFOS_FIELD_PUBKEY = ?",
                ScalarHandler<Int>(), host, port, tsUpdated, pubKey)
        return (updated >= 1)
    }

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

    private fun getTimestamp(time: Instant? = null): Timestamp {
        return if (time == null) {
            Timestamp(Instant.now().toEpochMilli())
        } else {
            Timestamp(time.toEpochMilli())
        }
    }

    private fun createSchemaIfNotExists(connection: Connection) {
        QueryRunner().update(connection, "CREATE SCHEMA IF NOT EXISTS ${appConfig.databaseSchema}")
        connection.commit()
    }

    private fun createTablesIfNotExists(appConfig: AppConfig, connection: Connection) {
        val sqlCommands = SQLCommandsFactory.getSQLCommands(appConfig.databaseDriverclass)
        SQLDatabaseAccess(sqlCommands).initialize(connection, expectedDbVersion = 1) // TODO: [et]: Extract version
        connection.commit()
    }
}