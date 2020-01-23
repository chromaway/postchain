// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain

import net.postchain.base.data.*
import net.postchain.config.app.AppConfig
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.commons.dbutils.QueryRunner
import java.sql.Connection
import javax.sql.DataSource

class StorageBuilder {

    companion object {

        fun buildStorage(appConfig: AppConfig, nodeIndex: Int, wipeDatabase: Boolean = false): BaseStorage {
            val sqlCommands = SQLCommandsFactory.getSQLCommands(appConfig.databaseDriverclass)

            val db = when (sqlCommands) {
                is PostgreSQLCommands -> PostgreSQLDatabaseAccess(sqlCommands)
                is SAPHanaSQLCommands -> SAPHanaSQLDatabaseAccess(sqlCommands)
                else -> SQLDatabaseAccess(sqlCommands)
            }

            val initSchemaWriteDataSource = createBasicDataSource(appConfig, false)

            if (wipeDatabase) {
                wipeDatabase(initSchemaWriteDataSource, appConfig, sqlCommands)
            } else {
                createSchemaIfNotExists(initSchemaWriteDataSource, appConfig.databaseSchema, sqlCommands)
            }
            initSchemaWriteDataSource.close()

            // Read DataSource
            val readDataSource = createBasicDataSource(appConfig).apply {
                defaultAutoCommit = true
                maxTotal = 2
                defaultReadOnly = true
            }

            // Write DataSource
            val writeDataSource = createBasicDataSource(appConfig).apply {
                maxWaitMillis = 0
                defaultAutoCommit = false
                maxTotal = 1
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

        private fun createBasicDataSource(appConfig: AppConfig, withSchema: Boolean = true): BasicDataSource {
            return BasicDataSource().apply {
                driverClassName = appConfig.databaseDriverclass
                url = appConfig.databaseUrl // ?loggerLevel=OFF
                username = appConfig.databaseUsername
                password = appConfig.databasePassword
                defaultAutoCommit = false

                if (withSchema) {
                    defaultSchema = appConfig.databaseSchema
                }
            }

        }

        private fun wipeDatabase(dataSource: DataSource, appConfig: AppConfig, sqlCommands: SQLCommands) {
            dataSource.connection.use { connection ->
                QueryRunner().let { query ->
                    if (isSchemaExists(connection, appConfig.databaseSchema)) {
                        query.update(connection, sqlCommands.dropSchemaCascade(appConfig.databaseSchema))
                    }
                    query.update(connection, sqlCommands.createSchema(appConfig.databaseSchema))
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