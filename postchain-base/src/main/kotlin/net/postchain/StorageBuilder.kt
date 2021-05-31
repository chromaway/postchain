// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain

import net.postchain.base.Storage
import net.postchain.base.data.BaseStorage
import net.postchain.base.data.DatabaseAccess
import net.postchain.base.data.DatabaseAccessFactory
import net.postchain.config.app.AppConfig
import org.apache.commons.dbcp2.BasicDataSource
import javax.sql.DataSource

object StorageBuilder {

    fun buildStorage(appConfig: AppConfig, nodeIndex: Int, wipeDatabase: Boolean = false): Storage {
        val db = DatabaseAccessFactory.createDatabaseAccess(appConfig.databaseDriverclass)
        initStorage(appConfig, wipeDatabase, db)

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
            maxTotal = 2
        }

        return BaseStorage(
                readDataSource,
                writeDataSource,
                nodeIndex,
                db,
                db.isSavepointSupported())
    }

    private fun initStorage(appConfig: AppConfig, wipeDatabase: Boolean, db: DatabaseAccess) {
        val initDataSource = createBasicDataSource(appConfig)

        if (wipeDatabase) {
            wipeDatabase(initDataSource, appConfig, db)
        }

        createSchemaIfNotExists(initDataSource, appConfig.databaseSchema, db)
        createTablesIfNotExists(initDataSource, db)
        initDataSource.close()
    }

    private fun setCurrentSchema(dataSource: DataSource, schema: String, db: DatabaseAccess) {
        dataSource.connection.use { connection ->
            db.setCurrentSchema(connection, schema)
            connection.commit()
        }
    }

    private fun createBasicDataSource(appConfig: AppConfig, withSchema: Boolean = true): BasicDataSource {
        return BasicDataSource().apply {
            driverClassName = appConfig.databaseDriverclass
            url = appConfig.databaseUrl // + "?loggerLevel=TRACE&loggerFile=db.log"
            username = appConfig.databaseUsername
            password = appConfig.databasePassword
            defaultAutoCommit = false

            if (withSchema) {
                defaultSchema = appConfig.databaseSchema
            }
        }

    }

    private fun wipeDatabase(dataSource: DataSource, appConfig: AppConfig, db: DatabaseAccess) {
        dataSource.connection.use { connection ->
            if (db.isSchemaExists(connection, appConfig.databaseSchema)) {
                db.dropSchemaCascade(connection, appConfig.databaseSchema)
                connection.commit()
            }
        }
    }

    private fun createSchemaIfNotExists(dataSource: DataSource, schema: String, db: DatabaseAccess) {
        dataSource.connection.use { connection ->
            if (!db.isSchemaExists(connection, schema)) {
                db.createSchema(connection, schema)
                connection.commit()
            }
        }
    }

    private fun createTablesIfNotExists(dataSource: DataSource, db: DatabaseAccess) {
        dataSource.connection.use { connection ->
            db.initializeApp(connection, expectedDbVersion = 2) // TODO: [et]: Extract version
            connection.commit()
        }
    }
}