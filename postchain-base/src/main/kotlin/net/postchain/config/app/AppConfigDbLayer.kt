package net.postchain.config.app

import net.postchain.base.PeerInfo
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.base.data.SQLDatabaseAccess.Companion.TABLE_PEERINFOS
import net.postchain.base.data.SQLDatabaseAccess.Companion.TABLE_PEERINFOS_FIELD_HOST
import net.postchain.base.data.SQLDatabaseAccess.Companion.TABLE_PEERINFOS_FIELD_PORT
import net.postchain.base.data.SQLDatabaseAccess.Companion.TABLE_PEERINFOS_FIELD_PUBKEY
import net.postchain.common.hexStringToByteArray
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.MapListHandler
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
        val rawPeerInfos: MutableList<MutableMap<String, Any>> = QueryRunner().query(
                connection, "SELECT * FROM $TABLE_PEERINFOS", MapListHandler())

        return rawPeerInfos.map {
            PeerInfo(
                    it[TABLE_PEERINFOS_FIELD_HOST] as String,
                    it[TABLE_PEERINFOS_FIELD_PORT] as Int,
                    (it[TABLE_PEERINFOS_FIELD_PUBKEY] as String).hexStringToByteArray())
        }.toTypedArray()
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