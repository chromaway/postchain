package net.postchain.e2e.tools

import org.apache.commons.dbcp2.BasicDataSource
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.ColumnListHandler
import org.apache.commons.dbutils.handlers.ScalarHandler

class DbTool(
        private val databaseUrl: String,
        private val databaseSchema: String
) : AutoCloseable {

    private val dataSource = buildDataSource(
            "org.postgresql.Driver",
            databaseUrl,
            databaseSchema,
            "postchain",
            "postchain")

    fun getTxsCount(): Long {
        return dataSource.connection.use { connection ->
            QueryRunner().query(connection, "SELECT COUNT(*) FROM transactions", ScalarHandler<Long>())
        }
    }

    fun getCities(): List<String> {
        return dataSource.connection.use { connection ->
            QueryRunner().query(connection, "SELECT name FROM $databaseSchema.\"c100.city\"", ColumnListHandler<String>())
        }
    }

    fun getPeerIds(): List<ByteArray> {
        return dataSource.connection.use { connection ->
            QueryRunner().query(connection, "SELECT pubkey FROM $databaseSchema.\"c0.peer_info\"", ColumnListHandler<ByteArray>())
        }
    }

    fun getBlockchainConfigsHeights(): List<Long> {
        return dataSource.connection.use { connection ->
            QueryRunner().query(connection, "SELECT height FROM $databaseSchema.\"c0.blockchain_configuration\"", ColumnListHandler<Long>())
        }
    }

    private fun buildDataSource(
            dbDriverClassName: String,
            dbUrl: String,
            dbSchema: String,
            dbUsername: String,
            dbPassword: String
    ): BasicDataSource {

        return BasicDataSource().apply {
            addConnectionProperty("currentSchema", dbSchema)
            driverClassName = dbDriverClassName
            url = dbUrl // + "?loggerLevel=TRACE&loggerFile=db.log"
            username = dbUsername
            password = dbPassword
            defaultAutoCommit = true
            maxTotal = 5
            defaultReadOnly = true
        }
    }

    override fun close() {
        dataSource.close()
    }
}