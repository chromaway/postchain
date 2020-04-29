// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools

import com.google.gson.GsonBuilder
import mu.KLogging
import net.postchain.StorageBuilder
import net.postchain.base.BlockchainRid
import net.postchain.base.gtxml.TestType
import net.postchain.config.app.AppConfig
import net.postchain.config.node.NodeConfigurationProviderFactory
import net.postchain.core.NODE_ID_TODO
import net.postchain.core.UserMistake
import net.postchain.core.byteArrayKeyOf
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.devtools.utils.configuration.NodeSeqNumber
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.gtvml.GtvMLParser
import net.postchain.gtx.gtxml.GTXMLTransactionParser
import net.postchain.gtx.gtxml.TransactionContext
import java.io.File
import java.io.StringReader
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBElement
import javax.xml.bind.util.ValidationEventCollector

/**
 * TODO: [et]: Maybe redesign this implementation based on [IntegrationTestSetup] currently
 */
class TestLauncher : IntegrationTestSetup() {

    companion object : KLogging()

    private val jaxbContext = JAXBContext.newInstance("net.postchain.base.gtxml")

    class TransactionFailure(val blockHeight: Long, val txIdx: Long,
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

    private fun createTestNode(configFile: String, blockchainConfigFile: String): PostchainTestNode {
        val appConfig = AppConfig.fromPropertiesFile(configFile)
        val nodeConfigProvider = NodeConfigurationProviderFactory.createProvider(appConfig)

        /*
        // TODO: Fix this hack
        nodeConfig.setProperty("api.port", -1) // FYI: Disabling Rest API in test mode
        nodeConfig.setProperty("node.0.id", nodeConfig.getProperty("test.node.0.id"))
        nodeConfig.setProperty("node.0.host", nodeConfig.getProperty("test.node.0.host"))
        nodeConfig.setProperty("node.0.port", nodeConfig.getProperty("test.node.0.port"))
        nodeConfig.setProperty("node.0.pubkey", nodeConfig.getProperty("test.node.0.pubkey"))
        nodeConfig.setProperty("database.schema", nodeConfig.getProperty("test.database.schema"))
        */

        val blockchainConfig = GtvMLParser.parseGtvML(
                File(blockchainConfigFile).readText())

        val chainId = nodeConfigProvider.getConfiguration().activeChainIds.first().toLong()

        // Wiping of database
        StorageBuilder.buildStorage(appConfig, NODE_ID_TODO, true).close()

        return PostchainTestNode(nodeConfigProvider).apply {
            val blockchainRID = addBlockchain(chainId, blockchainConfig)
            mapBlockchainRID(chainId, blockchainRID)
            startBlockchain()
            nodes.add(this)
            nodeMap[NodeSeqNumber(0)] = this
        }
    }

    data class EnqueuedTx(
            val txIdx: Long,
            val txRID: ByteArray,
            val isFailure: Boolean
    )

    fun runXMLGTXTests(xml: String,
                       blockchainRID: String,
                       nodeConfigFile: String? = null,
                       blockchainConfigFile: String? = null
    ): TestOutput {
        try {
            return _runXMLGTXTests(xml, blockchainRID, nodeConfigFile, blockchainConfigFile)
        } finally {
            tearDown()
        }
    }

    private fun _runXMLGTXTests(xml: String,
                                blockchainRIDStr: String,
                                nodeConfigFile: String? = null,
                                blockchainConfigFile: String? = null
    ): TestOutput {
        val blockchainRID = BlockchainRid.buildFromHex(blockchainRIDStr)
        val node: PostchainTestNode
        val testType: TestType
        try {
            // TODO: Resolve nullability here and above: !! vs ?.
            node = createTestNode(nodeConfigFile!!, blockchainConfigFile!!)
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
                blockchainRID,
                mapOf(
                        "user1pub" to gtv(pubKey(0)),
                        "user2pub" to gtv(user2pub),
                        "user3pub" to gtv(user3pub),
                        "Alice" to gtv(pubKey(0)),
                        "Bob" to gtv(user2pub),
                        "Claire" to gtv(user3pub)
                ),
                true,
                mapOf(
                        pubKey(0).byteArrayKeyOf() to cryptoSystem.buildSigMaker(pubKey(0), privKey(0)),
                        user2pub.byteArrayKeyOf() to cryptoSystem.buildSigMaker(user2pub, user2priv),
                        user3pub.byteArrayKeyOf() to cryptoSystem.buildSigMaker(user3pub, user3priv)
                )
        )

        val failures = mutableListOf<TransactionFailure>()

        for ((blockIdx, block) in testType.block.withIndex()) {
            logger.info("Block will be processed")
            val blockNum = blockIdx.toLong() + 1

            val enqueued = mutableListOf<EnqueuedTx>()
            for ((txIdx, txXml) in block.transaction.withIndex()) {
                try {
                    val gtxData = GTXMLTransactionParser.parseGTXMLTransaction(txXml, txContext, cryptoSystem)
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
                    val engine = node.getBlockchainInstance().getEngine()
                    val reason = engine.getTransactionQueue().getRejectionReason(txRID)
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