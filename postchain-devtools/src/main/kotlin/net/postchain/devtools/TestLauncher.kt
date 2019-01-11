package net.postchain.devtools

import com.google.gson.GsonBuilder
import mu.KLogging
import net.postchain.base.gtxml.TestType
import net.postchain.common.hexStringToByteArray
import net.postchain.config.CommonsConfigurationFactory
import net.postchain.core.UserMistake
import net.postchain.core.byteArrayKeyOf
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.gtx.gtx
import net.postchain.gtx.gtxml.GTXMLTransactionParser
import net.postchain.gtx.gtxml.TransactionContext
import java.io.StringReader
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBElement
import javax.xml.bind.util.ValidationEventCollector

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
    ) {
        fun toJSON(): String {
            val gson = GsonBuilder().create()!!
            return gson.toJson(this)
        }
    }

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

    data class EnqueuedTx (
            val txIdx: Long,
            val txRID: ByteArray,
            val isFailure: Boolean
    )

    fun runXMLGTXTests(xml: String,
                       blockchainRID: String?,
                       configFile: String? = null
    ): TestOutput {
        try {
            return _runXMLGTXTests(xml, blockchainRID, configFile)
        } finally {
            tearDown()
        }
    }

    private fun _runXMLGTXTests(xml: String,
                       blockchainRID: String?,
                       configFile: String? = null
    ): TestOutput {
        val node: SingleChainTestNode
        val testType: TestType
        try {
            node = createTestNode(configFile!!)
        } catch (e: Exception) {
            return TestOutput(false, false, e, listOf())
        }
        try {
            testType = parseTest(xml)
        } catch (e: Exception) {
            return TestOutput(false, true, e, listOf())
        }

        val enqueuedTxs = mutableMapOf<Long, List<EnqueuedTx>>()

        // Genesis block
        buildBlockAndCommit(node)

        val user2pub = pubKey(1)
        val user2priv = privKey(1)
        val user3pub = pubKey(2)
        val user3priv = privKey(2)

        val txContext = TransactionContext(
                blockchainRID?.hexStringToByteArray(),
                mapOf(
                        "user1pub" to gtx(pubKey(0)),
                        "user2pub" to gtx(user2pub),
                        "user3pub" to gtx(user3pub),
                        "Alice" to gtx(pubKey(0)),
                        "Bob" to gtx(user2pub),
                        "Claire" to gtx(user3pub)
                ),
                true,
                mapOf(
                        pubKey(0).byteArrayKeyOf() to cryptoSystem.makeSigner(pubKey(0), privKey(0)),
                        user2pub.byteArrayKeyOf() to cryptoSystem.makeSigner(user2pub, user2priv),
                        user3pub.byteArrayKeyOf() to cryptoSystem.makeSigner(user3pub, user3priv)
                )
        )

        val failures = mutableListOf<TransactionFailure>()

        for ((blockIdx, block) in testType.block.withIndex()) {
            logger.info("Block will be processed")
            val blockNum = blockIdx.toLong() + 1

            val enqueued = mutableListOf<EnqueuedTx>()
            for ((txIdx, txXml) in block.transaction.withIndex()) {
                try {
                    val gtxData = GTXMLTransactionParser.parseGTXMLTransaction(txXml, txContext)
                    val tx = enqueueTx(node, gtxData.serialize(), blockNum)
                    enqueued.add(EnqueuedTx(
                            txIdx.toLong(), tx!!.getRID(), txXml.isFailure
                    ))
                    Unit
                } catch (e: Exception) {
                    if (!txXml.isFailure) {
                        failures.add(TransactionFailure(blockNum, txIdx.toLong(), e))
                    }
                }
            }
            enqueuedTxs[blockNum] = enqueued

            try {
                buildBlockAndCommit(node)
            } catch (e: Exception) {
                failures.add(TransactionFailure(blockNum, -1, e))
                return TestOutput(false, false, null,
                       failures)
            }
        }


        if (getBestHeight(node).toInt() != testType.block.size) {
            failures.add(TransactionFailure(-1, -1,
                            Exception("Unexpected error: not all blocks were built")))
        }


        for (blockHeight in 1..testType.block.size) {
            val actualRIDs = getTxRidsAtHeight(node, blockHeight.toLong()).map { it.byteArrayKeyOf() }.toSet()

            enqueuedTxs[blockHeight.toLong()]!!.forEach {
                val txRID = it.txRID.byteArrayKeyOf()
                val present = actualRIDs.contains(txRID)
                if (present && it.isFailure) {
                    failures.add(TransactionFailure(blockHeight.toLong(), it.txIdx,
                            Exception("Transaction should fail")))
                } else if (!present && !it.isFailure) {
                    val reason = node.getBlockchainInstance().networkAwareTxQueue.getRejectionReason(txRID)
                    failures.add(TransactionFailure(blockHeight.toLong(), it.txIdx, reason))
                }
            }
        }

        return TestOutput(
                failures.size == 0,
                false,
                null,
                failures
        )
    }

    private fun parseTest(xml: String): TestType {
        val validator = ValidationEventCollector()
        val jaxbElement = jaxbContext.createUnmarshaller()
                .apply { eventHandler = validator }
                .unmarshal(StringReader(xml)) as JAXBElement<*>

        if (validator.hasEvents()) {
            throw UserMistake(validator.events.first().message)
        }

        return jaxbElement.value as TestType
    }

}