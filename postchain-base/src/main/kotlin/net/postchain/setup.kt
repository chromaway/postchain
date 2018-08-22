// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain

import net.postchain.base.*
import net.postchain.base.data.BaseStorage
import net.postchain.base.data.BaseTransactionQueue
import net.postchain.core.*
import net.postchain.core.BlockchainEngine
import net.postchain.gtx.encodeGTXValue
import net.postchain.gtx.GTXValue
import org.apache.commons.configuration2.Configuration
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.commons.dbutils.QueryRunner
import javax.sql.DataSource

// TODO: remove legacy
fun getBlockchainConfiguration(config: Configuration, chainId: Long, nodeIndex: Int): BlockchainConfiguration {
    val bcfClass = Class.forName(config.getString("configurationfactory"))
    val factory = (bcfClass.newInstance() as BlockchainConfigurationFactory)

    // TODO: BaseBlockchainConfigurationData is where the config magic happens
    val baseConfig = BaseBlockchainConfigurationData.readFromCommonsConfiguration(
            config, chainId, nodeIndex
    )
    return factory.makeBlockchainConfiguration(baseConfig,
            BaseBlockchainContext(baseConfig.blockchainRID, nodeIndex, chainId))
}

class TestNodeEngine(val engine: BlockchainEngine,
                     val txQueue: TransactionQueue,
                     val blockchainConfiguration: BlockchainConfiguration,
                     val blockQueries: BaseBlockQueries) {

    fun close() {
        engine.close()
    }
}

// TODO: remove legacy
fun createDataLayer(config: Configuration, chainId: Long, nodeIndex: Int): TestNodeEngine {
    val blockchainSubset = config.subset("blockchain.$chainId")
    val blockchainConfiguration = getBlockchainConfiguration(blockchainSubset, chainId, nodeIndex)
    val storage = baseStorage(config, nodeIndex)
    withWriteConnection(storage, chainId, { blockchainConfiguration.initializeDB(it); true })

    val blockQueries = blockchainConfiguration.makeBlockQueries(storage)

    val txQueue = BaseTransactionQueue(blockchainSubset.getInt("queuecapacity", 2500))

    val engine = BaseBlockchainEngine(blockchainConfiguration, storage,
            chainId, txQueue)

    val node = TestNodeEngine(engine,
            txQueue,
            blockchainConfiguration,
            blockQueries as BaseBlockQueries)
    return node
}

fun createTestNodeEngine(infrastructure: BaseBlockchainInfrastructure, config: GTXValue, bc: BlockchainContext): TestNodeEngine {
    val rawConfig = encodeGTXValue(config)
    val blockchainConfiguration = infrastructure.makeBlockchainConfiguration(rawConfig, bc)

    val engine = infrastructure.makeBlockchainEngine(blockchainConfiguration)

    val node = TestNodeEngine(engine,
            engine.getTransactionQueue(),
            blockchainConfiguration,
            engine.getBlockQueries() as BaseBlockQueries)
    return node
}

fun baseStorage(config: Configuration, nodeIndex: Int): BaseStorage {
    val writeDataSource = createBasicDataSource(config)
    writeDataSource.maxWaitMillis = 0
    writeDataSource.defaultAutoCommit = false
    writeDataSource.maxTotal = 1
    if (config.getBoolean("database.wipe", false)) {
        wipeDatabase(writeDataSource, config)
    }
    createSchemaIfNotExists(writeDataSource, config.getString("database.schema"))

    val readDataSource = createBasicDataSource(config)
    readDataSource.defaultAutoCommit = true
    readDataSource.maxTotal = 2
    readDataSource.defaultReadOnly = true

    val storage = BaseStorage(writeDataSource, readDataSource, nodeIndex)
    return storage
}

fun createBasicDataSource(config: Configuration): BasicDataSource {
    val dataSource = BasicDataSource()
    val schema = config.getString("database.schema", "public")
    dataSource.addConnectionProperty("currentSchema", schema)
    dataSource.driverClassName = config.getString("database.driverclass")
    dataSource.url = config.getString("database.url")
    dataSource.username = config.getString("database.username")
    dataSource.password = config.getString("database.password")
    dataSource.defaultAutoCommit = false
    return dataSource
}

fun wipeDatabase(dataSource: DataSource, config: Configuration) {
    val schema = config.getString("database.schema", "public")
    val queryRunner = QueryRunner()
    val conn = dataSource.connection
    queryRunner.update(conn, "DROP SCHEMA IF EXISTS $schema CASCADE")
    queryRunner.update(conn, "CREATE SCHEMA $schema")
    conn.commit()
    conn.close()
}

private fun createSchemaIfNotExists(dataSource: DataSource, schema: String) {
    val queryRunner = QueryRunner()
    val conn = dataSource.connection
    try {
        queryRunner.update(conn, "CREATE SCHEMA IF NOT EXISTS $schema")
        conn.commit()
    } finally {
        conn.close()
    }
}