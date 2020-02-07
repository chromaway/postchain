package net.postchain.e2e.tools

import org.apache.commons.dbcp2.BasicDataSource
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.ScalarHandler
import java.sql.Connection

class DbTool(
        private val databaseUrl: String,
        private val databaseSchema: String
) {

    private val databaseDriverclass = "org.postgresql.Driver"
    private val databaseUsername = "postchain"
    private val databasePassword = "postchain"

    private lateinit var connection: Connection

    fun getTxsCount(): Long {
        return withReadConnection {
            QueryRunner().query(it, "SELECT COUNT(*) FROM transactions", ScalarHandler<Long>())
        }
    }

    private fun <Result> withReadConnection(action: (Connection) -> Result): Result {
        val connection = openReadConnection()
        try {
            return action(connection)
        } finally {
            closeReadConnection(connection)
        }
    }

    private fun openReadConnection(): Connection {
        return BasicDataSource().apply {
            addConnectionProperty("currentSchema", databaseSchema)
            driverClassName = databaseDriverclass
            url = databaseUrl // + "?loggerLevel=TRACE&loggerFile=db.log"
            username = databaseUsername
            password = databasePassword
            defaultAutoCommit = true
            maxTotal = 10
            defaultReadOnly = true
        }.connection
    }

    private fun closeReadConnection(connection: Connection) {
        connection.close()
    }
}