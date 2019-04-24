// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain

import net.postchain.base.data.BaseStorage
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.config.node.NodeConfig
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.commons.dbutils.QueryRunner
import javax.sql.DataSource

class StorageBuilder {

    companion object {

        private val dbAccess = SQLDatabaseAccess()

        fun buildStorage(nodeConfig: NodeConfig, nodeIndex: Int, wipeDatabase: Boolean = false): BaseStorage {
            // Read DataSource
            val readDataSource = createBasicDataSource(nodeConfig).apply {
                defaultAutoCommit = true
                maxTotal = 2
                defaultReadOnly = true
            }

            // Write DataSource
            val writeDataSource = createBasicDataSource(nodeConfig).apply {
                maxWaitMillis = 0
                defaultAutoCommit = false
                maxTotal = 1
            }

            if (wipeDatabase) {
                wipeDatabase(writeDataSource, nodeConfig)
            }

            createSchemaIfNotExists(writeDataSource, nodeConfig.databaseSchema)
            createTablesIfNotExists(writeDataSource)

            return BaseStorage(readDataSource, writeDataSource, nodeIndex, SQLDatabaseAccess())
        }

        private fun createBasicDataSource(nodeConfig: NodeConfig): BasicDataSource {
            return BasicDataSource().apply {
                addConnectionProperty("currentSchema", nodeConfig.databaseSchema)
                driverClassName = nodeConfig.databaseDriverclass
                url = "${nodeConfig.databaseUrl}?loggerLevel=OFF"
                username = nodeConfig.databaseUsername
                password = nodeConfig.databasePassword
                defaultAutoCommit = false
            }
        }

        private fun wipeDatabase(dataSource: DataSource, nodeConfig: NodeConfig) {
            dataSource.connection.use { connection ->
                QueryRunner().let { query ->
                    query.update(connection, "DROP SCHEMA IF EXISTS ${nodeConfig.databaseSchema} CASCADE")
                    query.update(connection, "CREATE SCHEMA ${nodeConfig.databaseSchema}")
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

    }
}