// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain

import net.postchain.base.data.*
import net.postchain.config.CommonsConfigurationFactory
import org.apache.commons.configuration2.Configuration
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.commons.dbutils.QueryRunner
import java.sql.Connection
import javax.sql.DataSource

class StorageBuilder {

    companion object {

        fun buildStorage(config: Configuration, nodeIndex: Int, wipeDatabase: Boolean = false): BaseStorage {

            val sqlCommands = CommonsConfigurationFactory
                    .getSQLCommandsImplementation(config.getString("database.driverclass"))

            val db = when (sqlCommands) {
                is PostgreSQLCommands -> PostgreSQLDatabaseAccess(sqlCommands)
                is SAPHanaSQLCommands -> SAPHanaSQLDatabaseAccess(sqlCommands)
                else -> SQLDatabaseAccess(sqlCommands)
            }

            val initSchemaWriteDataSource = createBasicDataSource(config, false)

            if (wipeDatabase) {
                wipeDatabase(initSchemaWriteDataSource, config, sqlCommands)
            } else {
                createSchemaIfNotExists(initSchemaWriteDataSource,
                        schema(config), sqlCommands)
            }
            initSchemaWriteDataSource.close()

            // Read DataSource
            val readDataSource = createBasicDataSource(config).apply {
                defaultAutoCommit = true
                maxTotal = 2
                defaultReadOnly = true
            }

            // Write DataSource
            val writeDataSource = createBasicDataSource(config).apply {
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

        private fun createBasicDataSource(config: Configuration, withSchema: Boolean = true): BasicDataSource {
            return BasicDataSource().apply {
                driverClassName = config.getString("database.driverclass")
                url = "${config.getString("database.url")}" // ?loggerLevel=OFF
                username = config.getString("database.username")
                password = config.getString("database.password")
                defaultAutoCommit = false

                if (withSchema) {
                    defaultSchema = schema(config)
                }
            }

        }

        private fun wipeDatabase(dataSource: DataSource, config: Configuration, sqlCommands: SQLCommands) {
            dataSource.connection.use { connection ->
                QueryRunner().let { query ->
                    if (isSchemaExists(connection, schema(config))) {
                        query.update(connection, sqlCommands.dropSchemaCascade(schema(config)))
                    }
                    query.update(connection, sqlCommands.createSchema(schema(config)))
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

        private fun schema(config: Configuration): String {
            return config.getString("database.schema", "public")
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