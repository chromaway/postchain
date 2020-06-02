// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.config

import net.postchain.config.app.AppConfig
import org.apache.commons.dbcp2.BasicDataSource
import java.sql.Connection

@Deprecated("POS-128")
class SimpleDatabaseConnector(private val appConfig: AppConfig) : DatabaseConnector {

    override fun <Result> withReadConnection(action: (Connection) -> Result): Result {
        val connection = openReadConnection()

        try {
            return action(connection)
        } finally {
            closeReadConnection(connection)
        }
    }

    override fun <Result> withWriteConnection(action: (Connection) -> Result): Result {
        val connection = openWriteConnection()
        var doCommit = false
        try {
            val ret = action(connection)
            doCommit = true
            return ret
        } finally {
            closeWriteConnection(connection, doCommit)
        }
    }

    override fun openReadConnection(): Connection {
        return createReadDataSource(appConfig).connection
    }

    override fun closeReadConnection(connection: Connection) {
        connection.close()
    }

    override fun openWriteConnection(): Connection {
        return createWriteDataSource(appConfig).connection
    }

    override fun closeWriteConnection(connection: Connection, commit: Boolean) {
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
            url = appConfig.databaseUrl // + "?loggerLevel=TRACE&loggerFile=db.log"
            username = appConfig.databaseUsername
            password = appConfig.databasePassword
            defaultAutoCommit = false
        }
    }
}