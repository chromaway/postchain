// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest.reconfiguration

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.postchain.common.toHex
import net.postchain.core.Transaction
import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.testinfra.TestTransaction
import net.postchain.integrationtest.query

object TxChartHelper {

    private val gson = Gson()

    fun buildTxChart(
            node: PostchainTestNode,
            chainId: Long,
            maxHeight: Long = Long.MAX_VALUE,
            txPayloadName: String = "id"
    ): String {

        val chart = JsonObject()
        val blocks = JsonArray()
        chart.add("blocks", blocks)

        val height = minOf(
                node.query(chainId) { it.getBestHeight() } ?: -1L,
                maxHeight)

        (0..height).forEach { h ->
            val blockRid = node.query(chainId) { it.getBlockRid(h) }

            val block = JsonObject()
            block.addProperty("height", h)
            block.addProperty("block-rid", blockRid?.toHex())

            val txs = JsonArray()//mapper.createArrayNode()
            val txsRids = node.query(chainId) { it.getBlockTransactionRids(blockRid!!) }

            txsRids!!
                    .map { txRid ->
                        val tx = node.query(chainId) { it.getTransaction(txRid) }
                        val txPayload = (tx as? TestTransaction)?.id

                        JsonObject().apply {
                            addProperty("rid", txRid.toHex())
                            addProperty(txPayloadName, txPayload)
                        }
                    }.forEach {
                        txs.add(it)
                    }

            block.add("tx", txs)
            blocks.add(block)
        }

        //return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(chart)
        return gson.toJson(chart)
    }

    fun collectAllTxs(node: PostchainTestNode, chainId: Long): List<Transaction> {
        val txs = mutableListOf<Transaction>()

        val height = node.query(chainId) { it.getBestHeight() } ?: -1L
        (0..height).forEach { h ->
            val blockRid = node.query(chainId) { it.getBlockRid(h) }
            val txsRids = node.query(chainId) { it.getBlockTransactionRids(blockRid!!) }
            txsRids!!.forEach { txRid ->
                val tx = node.query(chainId) { it.getTransaction(txRid) }
                txs.add(tx!!)
            }
        }

        return txs
    }

}