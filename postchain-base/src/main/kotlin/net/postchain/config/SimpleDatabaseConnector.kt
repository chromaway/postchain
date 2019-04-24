package net.postchain.config

import net.postchain.config.app.AppConfig
import org.apache.commons.dbcp2.BasicDataSource
import java.sql.Connection

class SimpleDatabaseConnector(private val appConfig: AppConfig) {

    fun <Result> withReadConnection(action: (Connection) -> Result): Result {
        val connection = openReadConnection()

        try {
            return action(connection)
        } finally {
            closeReadConnection(connection)
        }
    }

    fun <Result> withWriteConnection(action: (Connection) -> Result): Result {
        val connection = openWriteConnection()

        try {
            return action(connection)
        } finally {
            closeWriteConnection(connection)
        }
    }

    fun openReadConnection(): Connection {
        return createReadDataSource(appConfig).connection
    }

    fun closeReadConnection(connection: Connection) {
        connection.close()
    }

    fun openWriteConnection(): Connection {
        return createWriteDataSource(appConfig).connection
    }

    fun closeWriteConnection(connection: Connection, commit: Boolean = true) {
        if (commit) {
            connection.commit()
        } else {
            connection.rollback()
        }

        connection.close()
    }

    private fun createReadDataSource(appConfig: AppConfig): BasicDataSource {
        return createBasicDataSource(appConfig).apply {
            defaultAutoCommit = true
            maxTotal = 2
            defaultReadOnly = true
        }
    }

    private fun createWriteDataSource(appConfig: AppConfig): BasicDataSource {
        return createBasicDataSource(appConfig).apply {
            maxWaitMillis = 0
            defaultAutoCommit = false
            maxTotal = 1
        }
    }

    private fun createBasicDataSource(appConfig: AppConfig): BasicDataSource {
        return BasicDataSource().apply {
            addConnectionProperty("currentSchema", appConfig.databaseSchema)
            driverClassName = appConfig.databaseDriverclass
            url = "${appConfig.databaseUrl}?loggerLevel=TRACE&loggerFile=db.log"
            username = appConfig.databaseUsername
            password = appConfig.databasePassword
            defaultAutoCommit = false
        }
    }
}