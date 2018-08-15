package net.postchain.test

import mu.KLogging
import net.postchain.base.gtxml.TestType
import net.postchain.common.hexStringToByteArray
import net.postchain.gtx.GTXBlockchainConfigurationFactory
import net.postchain.gtx.StandardOpsGTXModule
import net.postchain.gtx.gtxml.GTXMLTransactionParser
import net.postchain.gtx.gtxml.TransactionContext
import java.io.StringReader
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBElement


class TestLauncher : IntegrationTest() {

    companion object : KLogging()

    private val jaxbContext = JAXBContext.newInstance("net.postchain.base.gtxml")

    fun runXMLGTXTests(xml: String, blockchainRID: String?, configFile: String? = null): Boolean {
        var res = true

        configOverrides.setProperty(
                "blockchain.1.configurationfactory",
                GTXBlockchainConfigurationFactory::class.qualifiedName)
        configOverrides.setProperty(
                "blockchain.1.gtx.modules",
                listOf(StandardOpsGTXModule::class.qualifiedName))

        val dataLayer = createDataLayer(0, configFile = configFile ?: DEFAULT_CONFIG_FILE)
        val testType = parseTest(xml)

        var blockNum = 0L
        val enqueuedTxsRids = mutableMapOf<Long, List<ByteArray>>()

        // Genesis block
        buildBlockAndCommit(dataLayer)
        blockNum++

        // Blocks of test
        testType.block.forEach {
            logger.info("Block will be processed")

            val enqueued = mutableListOf<ByteArray>()
            it.transaction.forEach {
                if (it.isFailure) {
                    logger.info("Transaction will not be processed due to it marked as 'failure'")
                } else {
                    logger.info("Transaction will be processed")

                    val txContext = TransactionContext(blockchainRID?.hexStringToByteArray())
                    val gtxData = GTXMLTransactionParser.parseGTXMLTransaction(it, txContext)
                    val tx = enqueueTx(dataLayer, gtxData.serialize(), blockNum)
                    enqueued.add(tx!!.getRID())
                }
            }
            enqueuedTxsRids[blockNum] = enqueued

            buildBlockAndCommit(dataLayer)
            blockNum++
        }

        // Assert height
        res = res and (blockNum - 1 == getBestHeight(dataLayer))

        // Assert consumed txs
        res = res and enqueuedTxsRids.all { entry ->
            entry.value.toTypedArray().contentDeepEquals(
                    getTxRidsAtHeight(dataLayer, entry.key))
        }

        return res
    }

    private fun parseTest(xml: String): TestType {
        val jaxbElement = jaxbContext
                .createUnmarshaller()
                .unmarshal(StringReader(xml)) as JAXBElement<*>

        return jaxbElement.value as TestType
    }
}