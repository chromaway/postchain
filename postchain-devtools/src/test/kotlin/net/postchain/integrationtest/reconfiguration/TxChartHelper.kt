package net.postchain.integrationtest.reconfiguration

import com.fasterxml.jackson.databind.ObjectMapper
import net.postchain.common.toHex
import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.testinfra.TestTransaction
import net.postchain.integrationtest.query

object TxChartHelper {

    fun buildTxChart(node: PostchainTestNode, chainId: Long, txPayloadName: String = "id"): String {
        val mapper = ObjectMapper()
        val chart = mapper.createObjectNode()
        val blocks = mapper.createArrayNode()
        chart.set("blocks", blocks)

        val height = node.query(chainId) { it.getBestHeight() } ?: -1L
        (0..height).forEach { h ->
            val blockRid = node.query(chainId) { it.getBlockRids(h) }

            val block = mapper.createObjectNode()
            block.put("height", h)
            block.put("block-rid", blockRid?.toHex())

            val txs = mapper.createArrayNode()
            val txsRids = node.query(chainId) { it.getBlockTransactionRids(blockRid!!) }

            txsRids!!
                    .map { txRid ->
                        val tx = node.query(chainId) { it.getTransaction(txRid) }
                        val txPayload = (tx as? TestTransaction)?.id
                        mapper.createObjectNode().apply {
                            put("rid", txRid.toHex())
                            put(txPayloadName, txPayload)
                        }
                    }.forEach {
                        txs.add(it)
                    }

            block.set("tx", txs)
            blocks.add(block)
        }

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(chart)
    }

}