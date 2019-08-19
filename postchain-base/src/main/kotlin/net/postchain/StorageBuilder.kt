// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain

import net.postchain.base.data.*
import net.postchain.config.node.NodeConfig
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.commons.dbutils.QueryRunner
import java.sql.Connection
import javax.sql.DataSource

class StorageBuilder {

    companion object {

        fun buildStorage(nodeConfig: NodeConfig, nodeIndex: Int, wipeDatabase: Boolean = false): BaseStorage {
            val sqlCommands = SQLCommandsFactory.getSQLCommands(nodeConfig.databaseDriverclass)

            val db = when (sqlCommands) {
                is PostgreSQLCommands -> PostgreSQLDatabaseAccess(sqlCommands)
                is SAPHanaSQLCommands -> SAPHanaSQLDatabaseAccess(sqlCommands)
                else -> SQLDatabaseAccess(sqlCommands)
            }

            val initSchemaWriteDataSource = createBasicDataSource(nodeConfig, false)

            if (wipeDatabase) {
                wipeDatabase(initSchemaWriteDataSource, nodeConfig, sqlCommands)
            } else {
                createSchemaIfNotExists(initSchemaWriteDataSource, nodeConfig.databaseSchema, sqlCommands)
            }
            initSchemaWriteDataSource.close()

            // Read DataSource
            val readDataSource = createBasicDataSource(nodeConfig).apply {
                defaultAutoCommit = true
                maxTotal = 200
                defaultReadOnly = true
            }

            // Write DataSource
            val writeDataSource = createBasicDataSource(nodeConfig).apply {
                maxWaitMillis = 0
                defaultAutoCommit = false
                maxTotal = 100
            }

            createTablesIfNotExists(writeDataSource, db)
            return BaseStorage(readDataSource, writeDataSource, nodeIndex, db, sqlCommands.isSavepointSupported())
        }

        private fun setCurrentSchema(dataSource: DataSource, schema: String, sqlCommands: SQLCommands) {
            dataSource.connection.use { connection ->
                QueryRunner().update(connection, sqlCommands.setCurrentSchema(schema))
                connection.commit()
            }
        }

        private fun createBasicDataSource(nodeConfig: NodeConfig, withSchema: Boolean = true): BasicDataSource {
            return BasicDataSource().apply {
                driverClassName = nodeConfig.databaseDriverclass
                url = nodeConfig.databaseUrl // ?loggerLevel=OFF
                username = nodeConfig.databaseUsername
                password = nodeConfig.databasePassword
                defaultAutoCommit = false

                if (withSchema) {
                    defaultSchema = nodeConfig.databaseSchema
                }
            }

        }

        private fun wipeDatabase(dataSource: DataSource, nodeConfig: NodeConfig, sqlCommands: SQLCommands) {
            dataSource.connection.use { connection ->
                QueryRunner().let { query ->
                    if (isSchemaExists(connection, nodeConfig.databaseSchema)) {
                        query.update(connection, sqlCommands.dropSchemaCascade(nodeConfig.databaseSchema))
                    }
                    query.update(connection, sqlCommands.createSchema(nodeConfig.databaseSchema))
                }
                connection.commit()
            }
        }

        private fun createSchemaIfNotExists(dataSource: DataSource, schema: String, sqlCommands: SQLCommands) {
            dataSource.connection.use { connection ->
                if (!isSchemaExists(connection, schema)) {
                    QueryRunner().update(connection, sqlCommands.createSchema(schema))
                    connection.commit()
                }
            }
        }

        private fun createTablesIfNotExists(dataSource: DataSource, dbAccess: SQLDatabaseAccess) {
            dataSource.connection.use { connection ->
                dbAccess.initialize(connection, expectedDbVersion = 1) // TODO: [et]: Extract version
                connection.commit()
            }
        }

        private fun isSchemaExists(conn: Connection, schema: String): Boolean {
            val rs = conn.metaData.schemas
            while (rs.next()) {
                if (rs.getString(1).toLowerCase() == schema.toLowerCase()) {
                    return true
                }
            }
            return false
        }
    }
}