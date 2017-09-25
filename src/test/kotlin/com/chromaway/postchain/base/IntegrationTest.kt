package com.chromaway.postchain.base

import com.chromaway.postchain.base.data.BaseBlockStore
import com.chromaway.postchain.base.data.BaseBlockchainConfiguration
import com.chromaway.postchain.base.data.BaseStorage
import com.chromaway.postchain.core.BlockQueries
import com.chromaway.postchain.core.BlockStore
import com.chromaway.postchain.core.BlockchainConfiguration
import com.chromaway.postchain.core.BlockchainConfigurationFactory
import com.chromaway.postchain.core.EContext
import com.chromaway.postchain.core.Transaction
import com.chromaway.postchain.core.TransactionFactory
import com.chromaway.postchain.core.TxEContext
import com.chromaway.postchain.core.UserError
import com.chromaway.postchain.ebft.BaseBlockchainEngine
import com.chromaway.postchain.ebft.BlockchainEngine
import mu.KLogging
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.builder.fluent.Configurations
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.commons.dbutils.QueryRunner
import org.junit.After
import org.junit.Assert.assertArrayEquals
import java.io.File
import javax.sql.DataSource

open class IntegrationTest {
    private val nodes = mutableListOf<DataLayer>()
    protected val blockStore = BaseBlockStore() as BlockStore
    companion object: KLogging()
//    private val privKeysHex = arrayOf("3132333435363738393031323334353637383930313233343536373839303131",
//            "3132333435363738393031323334353637383930313233343536373839303132",
//            "3132333435363738393031323334353637383930313233343536373839303133",
//            "3132333435363738393031323334353637383930313233343536373839303134")
//    protected val privKeys = privKeysHex.map { it.hexStringToByteArray() }
//
//    private val pubKeysHex = arrayOf("0350fe40766bc0ce8d08b3f5b810e49a8352fdd458606bd5fafe5acdcdc8ff3f57",
//            "035676109c54b9a16d271abeb4954316a40a32bcce023ac14c8e26e958aa68fba9",
//            "03f811d3e806e6d093a4bcce49c145ba78f9a4b2fbd167753ecab2a13530b081f8",
//            "03ef3f5be98d499b048ba28b247036b611a1ced7fcf87c17c8b5ca3b3ce1ee23a4")
//    protected val pubKeys = pubKeysHex.map { it.hexStringToByteArray() }

    fun privKey(index: Int): ByteArray {
        // private key index 0 is all zeroes except byte 16 which is 1
        // private key index 12 is all 12:s except byte 16 which is 1
        // reason for byte16=1 is that private key cannot be all zeroes
        return ByteArray(32, {if (it == 16) 1.toByte() else index.toByte()})
    }
    fun privKeyHex(index: Int): String {
        return privKey(index).toHex()
    }
    fun pubKey(index: Int): ByteArray {
        return secp256k1_derivePubKey(privKey(index))
    }
    fun pubKeyHex(index: Int): String {
        return pubKey(index).toHex()
    }

    class DataLayer(val engine: BlockchainEngine, val txQueue: TestTxQueue,
                    val readCtx: EContext, val blockchainConfiguration: BlockchainConfiguration,
                    private val dataSources: Array<BasicDataSource>, val blockQueries: BlockQueries) {
        fun close() {
            dataSources.forEach {
                it.close()
            }
            readCtx.conn.close()
        }
    }

    private class TestBlockchainConfigurationFactory : BlockchainConfigurationFactory {

        override fun makeBlockchainConfiguration(chainID: Long, config: Configuration):
                BlockchainConfiguration {
            return TestBlockchainConfiguration(chainID, config)
        }
    }

    protected class TestBlockchainConfiguration(chainID: Long, config: Configuration) : BaseBlockchainConfiguration(chainID, config) {
        val transactionFactory = TestTransactionFactory()

        override fun getTransactionFactory(): TransactionFactory {
            return transactionFactory
        }
    }

    class TestTransactionFactory : TransactionFactory {
        val specialTxs = mutableMapOf<Byte, Transaction>()

        override fun decodeTransaction(data: ByteArray): Transaction {
            if (specialTxs.containsKey(data[0])) {
                return specialTxs[data[0]]!!
            }
            val result = TestTransaction(data[0].toInt())
            assertArrayEquals(result.getRawData(), data)
            return result
        }
    }

    open class TestTransaction(val id: Int, val good: Boolean = true, val correct: Boolean = true) : Transaction {
        override fun isCorrect(): Boolean {
            return correct
        }

        override fun apply(ctx: TxEContext): Boolean {
            return good
        }

        override fun getRawData(): ByteArray {
            return ByteArray(id + 40, { id.toByte() })
        }

        override fun getRID(): ByteArray {
            return ByteArray(32, { id.toByte() })
        }
    }

    inner class ErrorTransaction(id: Int, private val applyThrows: Boolean, private val isCorrectThrows: Boolean) : TestTransaction(id) {
        override fun isCorrect(): Boolean {
            if (isCorrectThrows) throw UserError("Thrown from isCorrect()")
            return true
        }

        override fun apply(ctx: TxEContext): Boolean {
            if (applyThrows) throw UserError("Thrown from apply()")
            return true
        }
    }

    class TestTxQueue : TransactionQueue {
        private val q = ArrayList<Transaction>()

        override fun getTransactions(): Array<Transaction> {
            val result = Array(q.size, { q[it] })
            q.clear()
            return result
        }

        fun add(tx: Transaction) {
            q.add(tx)
        }
    }

    @After
    fun tearDown() {
        nodes.forEach { it.close() }
        nodes.clear()
        logger.debug("Closed nodes");
    }

    protected fun createEngines(count: Int): Array<DataLayer> {
        return Array(count, { createDataLayer(it) })
    }

    protected fun createDataLayer(nodeIndex: Int, nodeCount: Int = 1): DataLayer {
        val chainId = 1
        val configs = Configurations()
        val config = configs.properties(File("config.properties"))
        config.listDelimiterHandler = DefaultListDelimiterHandler(',')

        val factory = TestBlockchainConfigurationFactory()
        config.addProperty("blockchain.$chainId.signers", Array(nodeCount, {pubKeyHex(it)}).reduce({ acc, value -> "$acc,$value" }))
        // append nodeIndex to schema name
        config.setProperty("database.schema", config.getString("database.schema") + nodeIndex)
        config.setProperty("blockchain.$chainId.privkey", privKeyHex(nodeIndex))

        val blockchainConfiguration = factory.makeBlockchainConfiguration(chainId.toLong(), config)

        val writeDataSource = createBasicDataSource(config, true)
        writeDataSource.maxTotal = 1

        val readDataSource = createBasicDataSource(config)
        readDataSource.maxTotal = 2
        readDataSource.defaultReadOnly = true

        val storage = BaseStorage(writeDataSource, readDataSource)

        val readCtx = storage.openReadConnection(chainId)

        val txQueue = TestTxQueue()

        val engine = BaseBlockchainEngine(blockchainConfiguration, storage,
                chainId, txQueue)

        val blockQueries = blockchainConfiguration.makeBlockQueries(storage)

        val node = DataLayer(engine, txQueue, readCtx, blockchainConfiguration, arrayOf(readDataSource, writeDataSource), blockQueries)
        // keep list of nodes to close after test
        nodes.add(node)
        return node
    }

    private fun createBasicDataSource(config: Configuration, wipe: Boolean = false): BasicDataSource {
        val dataSource = BasicDataSource()
        val schema = config.getString("database.schema", "public")
        dataSource.addConnectionProperty("currentSchema", schema)
        dataSource.driverClassName = config.getString("database.driverclass")
        dataSource.url = config.getString("database.url")
        dataSource.username = config.getString("database.username")
        dataSource.password = config.getString("database.password")
        dataSource.defaultAutoCommit = false
        if (wipe) {
            wipeDatabase(dataSource, schema)

        }

        return dataSource
    }

    private fun wipeDatabase(dataSource: DataSource, schema: String) {
        val queryRunner = QueryRunner()
        val conn = dataSource.connection
        queryRunner.update(conn, "DROP SCHEMA IF EXISTS $schema CASCADE")
        queryRunner.update(conn, "CREATE SCHEMA $schema")
        // Implementation specific initialization.
        (blockStore as BaseBlockStore).initialize(EContext(conn, 1))
        conn.commit()
        conn.close()
    }

    protected fun createBasePeerCommConfiguration(nodeCount: Int, myIndex: Int): BasePeerCommConfiguration {
        val pubKeysToUse = Array<ByteArray>(nodeCount, { pubKey(it) })
        val peerInfos = Array(nodeCount, { PeerInfo("localhost", 53190 + it, pubKeysToUse[it]) })
        val privKey = privKey(myIndex)
        return BasePeerCommConfiguration(peerInfos, myIndex, SECP256K1CryptoSystem(), privKey)
    }

    protected fun arrayOfBasePeerCommConfigurations(count: Int): Array<BasePeerCommConfiguration> {
        return Array(count, { createBasePeerCommConfiguration(count, it) })
    }
}