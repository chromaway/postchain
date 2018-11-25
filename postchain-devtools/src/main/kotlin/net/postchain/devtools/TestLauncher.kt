package net.postchain.devtools

import mu.KLogging
import net.postchain.base.gtxml.TestType
import net.postchain.common.hexStringToByteArray
import net.postchain.config.CommonsConfigurationFactory
import net.postchain.core.byteArrayKeyOf
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.gtx.gtx
import net.postchain.gtx.gtxml.GTXMLTransactionParser
import net.postchain.gtx.gtxml.TransactionContext
import java.io.StringReader
import javax.management.modelmbean.XMLParseException
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBElement

/**
 * TODO: [et]: Maybe redesign this implementation based on [IntegrationTest] currently
 */
class TestLauncher : IntegrationTest() {

    companion object : KLogging()

    private val jaxbContext = JAXBContext.newInstance("net.postchain.base.gtxml")

    class TransactionFailure (val blockHeight: Long, val txIdx: Long,
                              val exception: Exception?)

    class TestOutput(
            val passed: Boolean,
            val malformedXML: Boolean,
            val initializationError: Exception?,
            val transactionFailures: List<TransactionFailure>
    )

    fun createTestNode(configFile: String): SingleChainTestNode {
        val config = CommonsConfigurationFactory.readFromFile(configFile)
        // TODO: Fix this hack
        config.setProperty("api.port", -1) // FYI: Disabling Rest API in test mode
        config.setProperty("node.0.id", config.getProperty("test.node.0.id"))
        config.setProperty("node.0.host", config.getProperty("test.node.0.host"))
        config.setProperty("node.0.port", config.getProperty("test.node.0.port"))
        config.setProperty("node.0.pubkey", config.getProperty("test.node.0.pubkey"))
        config.setProperty("database.schema", config.getProperty("test.database.schema"))

        return SingleChainTestNode(config).apply {
            startBlockchain()
            nodes.add(this)
        }
    }

    fun runXMLGTXTests(xml: String,
                       blockchainRID: String?,
                       configFile: String? = null,
                       testOutputFileName: String? = null
    ): TestOutput {
        var passed = true
        val node: SingleChainTestNode
        val testType: TestType
        try {
            node = createTestNode(configFile!!)
        } catch (e: Exception) {
            return TestOutput(false, false, e, listOf())
        }
        try {
            testType = parseTest(xml)
        } catch (e: XMLParseException) {
            return TestOutput(false, true, e, listOf())
        }

        var blockNum = 0L
        val enqueuedTxsRids = mutableMapOf<Long, List<ByteArray>>()

        // Genesis block
        buildBlockAndCommit(node)
        blockNum++

        val user1pub = pubKey(0)
        val user1priv = privKey(0)
        val user2pub = pubKey(1)
        val user2priv = privKey(1)
        val user3pub = pubKey(2)
        val user3priv = privKey(2)

        val txContext = TransactionContext(
                blockchainRID?.hexStringToByteArray(),
                mapOf(
                        "user1pub" to gtx(user1pub),
                        "user2pub" to gtx(user2pub),
                        "user3pub" to gtx(user3pub),
                        "Alice" to gtx(user1pub),
                        "Bob" to gtx(user2pub),
                        "Claire" to gtx(user3pub)
                ),
                true,
                mapOf(
                        user1pub.byteArrayKeyOf() to cryptoSystem.makeSigner(user1pub, user1priv),
                        user2pub.byteArrayKeyOf() to cryptoSystem.makeSigner(user2pub, user2priv),
                        user3pub.byteArrayKeyOf() to cryptoSystem.makeSigner(user3pub, user3priv)
                )
        )

        // Blocks of test
        testType.block.forEach {
            logger.info("Block will be processed")

            val enqueued = mutableListOf<ByteArray>()
            it.transaction.forEach {
                if (it.isFailure) {
                    logger.info("Transaction will not be processed due to it marked as 'failure'")
                } else {
                    logger.info("Transaction will be processed")

                    val gtxData = GTXMLTransactionParser.parseGTXMLTransaction(it, txContext)
                    val tx = enqueueTx(node, gtxData.serialize(), blockNum)
                    enqueued.add(tx!!.getRID())
                }
            }
            enqueuedTxsRids[blockNum] = enqueued

            buildBlockAndCommit(node)
            blockNum++
        }

        // Assert height
        passed = passed and (blockNum - 1 == getBestHeight(node))

        // Assert consumed txs
        passed = passed and enqueuedTxsRids.all { entry ->
            entry.value.toTypedArray().contentDeepEquals(
                    getTxRidsAtHeight(node, entry.key))
        }

        // Clearing
        tearDown()

        return passed
    }

    private fun parseTest(xml: String): TestType {
        val jaxbElement = jaxbContext
                .createUnmarshaller()
                .unmarshal(StringReader(xml)) as JAXBElement<*>

        return jaxbElement.value as TestType
    }

}