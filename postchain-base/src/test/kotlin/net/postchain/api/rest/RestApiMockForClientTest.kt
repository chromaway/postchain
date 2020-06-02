// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.api.rest

import mu.KLogging
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.controller.Query
import net.postchain.api.rest.controller.QueryResult
import net.postchain.api.rest.controller.RestApi
import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.ApiTx
import net.postchain.api.rest.model.TxRID
import net.postchain.base.ConfirmationProof
import net.postchain.base.cryptoSystem
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.core.*
import net.postchain.gtv.Gtv
import org.junit.After
import org.junit.Test

class RestApiMockForClientManual {
    val listenPort = 49545
    val basePath = "/basepath"
    private val blockchainRID = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a1"
    lateinit var restApi: RestApi

    companion object : KLogging()

    @After
    fun tearDown() {
        restApi.stop()
        logger.debug { "Stopped" }
    }

    @Test
    fun startMockRestApi() {
        val model = MockModel()
        restApi = RestApi(listenPort, basePath)
        restApi.attachModel(blockchainRID, model)
        logger.info("Ready to serve on port ${restApi.actualPort()}")
        Thread.sleep(600000) // Wait 10 minutes
    }

    class MockModel : Model {
        override val chainIID: Long
            get() = 5L
        private val blockchainRID = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a1"
        val statusUnknown = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        val statusRejected = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        val statusConfirmed = "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"
        val statusNotFound = "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"
        val statusWaiting = "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"

        val blocks = listOf<BlockDetail>(
                BlockDetail(
                        "blockRid001".toByteArray(),
                        blockchainRID.toByteArray(), "some header".toByteArray(),
                        0,
                        listOf<TxDetail>(),
                        "signatures".toByteArray(),
                        1574849700),
                BlockDetail(
                        "blockRid002".toByteArray(),
                        "blockRid001".toByteArray(),
                        "some other header".toByteArray(),
                        1,
                        listOf<TxDetail>(TxDetail("tx1".toByteArray(), "tx1".toByteArray(), "tx1".toByteArray())),
                        "signatures".toByteArray(),
                        1574849760),
                BlockDetail(
                        "blockRid003".toByteArray(),
                        "blockRid002".toByteArray(),
                        "yet another header".toByteArray(),
                        2,
                        listOf<TxDetail>(),
                        "signatures".toByteArray(),
                        1574849880),
                BlockDetail(
                        "blockRid004".toByteArray(),
                        "blockRid003".toByteArray(),
                        "guess what? Another header".toByteArray(),
                        3,
                        listOf<TxDetail>(
                                TxDetail("tx2".toByteArray(), "tx2".toByteArray(), "tx2".toByteArray()),
                                TxDetail("tx3".toByteArray(), "tx3".toByteArray(), "tx3".toByteArray()),
                                TxDetail("tx4".toByteArray(), "tx4".toByteArray(), "tx4".toByteArray())
                        ),
                        "signatures".toByteArray(),
                        1574849940)
        )

        override fun postTransaction(tx: ApiTx) {
            when (tx.tx) {
                "helloOK".toByteArray().toHex() -> return
                "hello400".toByteArray().toHex() -> throw UserMistake("expected error")
                "hello500".toByteArray().toHex() -> throw ProgrammerMistake("expected error")
                else -> throw ProgrammerMistake("unexpected error")
            }
        }

        override fun getTransaction(txRID: TxRID): ApiTx? {
            return when (txRID) {
                TxRID(statusUnknown.hexStringToByteArray()) -> null
                TxRID(statusConfirmed.hexStringToByteArray()) -> ApiTx("1234")
                else -> throw ProgrammerMistake("unexpected error")
            }
        }

        override fun getConfirmationProof(txRID: TxRID): ConfirmationProof? {
            return when (txRID) {
                TxRID(statusUnknown.hexStringToByteArray()) -> null
                else -> throw ProgrammerMistake("unexpected error")
            }
        }

        override fun getStatus(txRID: TxRID): ApiStatus {
            return when (txRID) {
                TxRID(statusUnknown.hexStringToByteArray()) -> ApiStatus(TransactionStatus.UNKNOWN)
                TxRID(statusWaiting.hexStringToByteArray()) -> ApiStatus(TransactionStatus.WAITING)
                TxRID(statusConfirmed.hexStringToByteArray()) -> ApiStatus(TransactionStatus.CONFIRMED)
                TxRID(statusRejected.hexStringToByteArray()) -> ApiStatus(TransactionStatus.REJECTED)
                else -> throw ProgrammerMistake("unexpected error")
            }
        }

        override fun query(query: Query): QueryResult {
            return QueryResult(when (query.json) {
                """{"a":"oknullresponse","c":3}""" -> ""
                """{"a":"okemptyresponse","c":3}""" -> """{}"""
                """{"a":"oksimpleresponse","c":3}""" -> """{"test":"hi"}"""
                """{"a":"usermistake","c":3}""" -> throw UserMistake("expected error")
                """{"a":"programmermistake","c":3}""" -> throw ProgrammerMistake("expected error")
                else -> throw ProgrammerMistake("unexpected error")
            })
        }

        //TODO Should tests in base have knowledge of GTV? If yes, convert getTransactionsInfo to use GTV
        override fun query(query: Gtv): Gtv {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun nodeQuery(subQuery: String): String = TODO()

        override fun getBlock(blockRID: ByteArray, partialTx: Boolean): BlockDetail? {
            return (blocks.filter { it.rid.contentEquals(blockRID) }).getOrNull(0)
        }

        override fun getBlocks(blockTime: Long, limit: Int, partialTx: Boolean): List<BlockDetail> {
            var queryBlocks = blocks.sortedBy { blockDetail -> blockDetail.height }
            return blocks.filter { blockDetail -> blockDetail.timestamp < blockTime }.subList(0, limit)
        }

        override fun getTransactionInfo(txRID: TxRID): TransactionInfoExt {
            val block = blocks.filter { block -> block.transactions.filter { tx -> cryptoSystem.digest(tx.data!!).contentEquals(txRID.bytes) }.size > 0 }[0]
            val tx = block.transactions.filter { tx -> cryptoSystem.digest(tx.data!!).contentEquals(txRID.bytes) }[0]
            return TransactionInfoExt(block.rid, block.height, block.header, block.witness, block.timestamp, cryptoSystem.digest(tx.data!!), tx.data!!.slice(IntRange(0, 4)).toByteArray(), tx.data!!)
        }

        override fun getTransactionsInfo(beforeTime: Long, limit: Int): List<TransactionInfoExt> {
            var queryBlocks = blocks
            var transactionsInfo: MutableList<TransactionInfoExt> = mutableListOf()
            queryBlocks = queryBlocks.sortedByDescending { blockDetail -> blockDetail.height }
            for (block in queryBlocks) {
                for (tx in block.transactions) {
                    transactionsInfo.add(TransactionInfoExt(block.rid, block.height, block.header, block.witness, block.timestamp, cryptoSystem.digest(tx.data!!), tx.data!!.slice(IntRange(0, 4)).toByteArray(), tx.data!!))
                }
            }
            return transactionsInfo.toList()
        }

        override fun debugQuery(subQuery: String?): String {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }
}
