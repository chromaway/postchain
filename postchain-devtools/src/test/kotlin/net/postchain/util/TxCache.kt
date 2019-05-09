package net.postchain.util

import net.postchain.common.toHex
import net.postchain.gtx.GTXTransaction
import net.postchain.integrationtest.multiple_chains.SinglePeerDependencyTest

/**
 * Used to fetch a Transaction created in a by the [MultiNodeDoubleChainBlockTestHelper]
 */
class TxCache(val internalTxList: List<GTXTransaction>) {

    fun getCachedTxRid(chainId: Int, nrOfChains: Int, height: Int, txPerBlock: Int, counter: Int): ByteArray {
        val baseIndex = height * txPerBlock + counter
        val index =  baseIndex * nrOfChains + chainId -1
        val expectedTxRid = internalTxList[index].getRID()
        SinglePeerDependencyTest.logger.debug(
                "Fetching cached TX with index: $index (chainId: $chainId, height: $height, TX pos: $counter):  TX RID: ${expectedTxRid.toHex()}")
        return expectedTxRid

    }
}