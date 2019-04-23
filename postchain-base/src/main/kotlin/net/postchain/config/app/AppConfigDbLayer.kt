package net.postchain.config.app

import net.postchain.base.PeerInfo
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.base.data.SQLDatabaseAccess.Companion.TABLE_PEERINFOS
import net.postchain.base.data.SQLDatabaseAccess.Companion.TABLE_PEERINFOS_FIELD_HOST
import net.postchain.base.data.SQLDatabaseAccess.Companion.TABLE_PEERINFOS_FIELD_PORT
import net.postchain.base.data.SQLDatabaseAccess.Companion.TABLE_PEERINFOS_FIELD_PUBKEY
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.MapListHandler
import org.apache.commons.dbutils.handlers.ScalarHandler
import java.sql.Connection

class AppConfigDbLayer(
        private val appConfig: AppConfig,
        private val connection: Connection
) {

    init {
        createSchemaIfNotExists(connection)
        createTablesIfNotExists(connection)
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
                    (it[TABLE_PEERINFOS_FIELD_PUBKEY] as String).hexStringToByteArray())
        }.toTypedArray()
    }

    fun addPeerInfo(host: String, port: Int, pubKey: String): Boolean {
        return QueryRunner().insert(
                connection,
                "INSERT INTO $TABLE_PEERINFOS " +
                        "($TABLE_PEERINFOS_FIELD_HOST, $TABLE_PEERINFOS_FIELD_PORT, $TABLE_PEERINFOS_FIELD_PUBKEY) " +
                        "VALUES (?, ?, ?) " +
                        "ON CONFLICT ($TABLE_PEERINFOS_FIELD_HOST, $TABLE_PEERINFOS_FIELD_PORT) " +
                        "DO UPDATE SET $TABLE_PEERINFOS_FIELD_PUBKEY = ? " +
                        "RETURNING true",
                ScalarHandler<Boolean>(), host, port, pubKey, pubKey)
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

    private fun createSchemaIfNotExists(connection: Connection) {
        QueryRunner().update(connection, "CREATE SCHEMA IF NOT EXISTS ${appConfig.databaseSchema}")
        connection.commit()
    }

    private fun createTablesIfNotExists(connection: Connection) {
        SQLDatabaseAccess().initialize(connection, expectedDbVersion = 1) // TODO: [et]: Extract version
        connection.commit()
    }
}