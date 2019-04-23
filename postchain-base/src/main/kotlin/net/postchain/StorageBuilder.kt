// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain

import net.postchain.base.data.BaseStorage
import net.postchain.base.data.SQLDatabaseAccess
import org.apache.commons.configuration2.Configuration
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.commons.dbutils.QueryRunner
import javax.sql.DataSource

class StorageBuilder {

    companion object {

        private val dbAccess = SQLDatabaseAccess()

        fun buildStorage(config: Configuration, nodeIndex: Int, wipeDatabase: Boolean = false): BaseStorage {
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

            if (wipeDatabase) {
                wipeDatabase(writeDataSource, config)
            }

            createSchemaIfNotExists(writeDataSource, config.getString("database.schema"))
            createTablesIfNotExists(writeDataSource)

            return BaseStorage(readDataSource, writeDataSource, nodeIndex, SQLDatabaseAccess())
        }

        private fun createBasicDataSource(config: Configuration): BasicDataSource {
            val databaseURL = System.getenv("POSTCHAIN_DB_URL") ?: config.getString("database.url")
            val databasePassword = System.getenv("POSTCHAIN_DB_PASSWORD") ?: config.getString("database.password")
            val databaseUsername = System.getenv("POSTCHAIN_DB_USERNAME") ?: config.getString("database.username")

            return BasicDataSource().apply {
                addConnectionProperty("currentSchema", schema(config))
                driverClassName = config.getString("database.driverclass")
                url = "${databaseURL}?loggerLevel=OFF"
                username = databaseUsername
                password = databasePassword
                defaultAutoCommit = false
            }
        }

        private fun wipeDatabase(dataSource: DataSource, config: Configuration) {
            dataSource.connection.use { connection ->
                QueryRunner().let { query ->
                    query.update(connection, "DROP SCHEMA IF EXISTS ${schema(config)} CASCADE")
                    query.update(connection, "CREATE SCHEMA ${schema(config)}")
                }
                connection.commit()
            }
        }

        private fun createSchemaIfNotExists(dataSource: DataSource, schema: String) {
            dataSource.connection.use { connection ->
                QueryRunner().update(connection, "CREATE SCHEMA IF NOT EXISTS $schema")
                connection.commit()
            }
        }

        private fun createTablesIfNotExists(dataSource: DataSource) {
            dataSource.connection.use { connection ->
                dbAccess.initialize(connection, expectedDbVersion = 1) // TODO: [et]: Extract version
                connection.commit()
            }
        }

        private fun schema(config: Configuration): String {
            return System.getenv("POSTCHAIN_DB_SCHEMA") ?:
                config.getString("database.schema", "public")
        }
    }
}