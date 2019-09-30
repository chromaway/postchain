package net.postchain.devtools

import mu.KLogging
import net.postchain.common.toHex
import net.postchain.gtx.GTXTransaction

/**
 * Used to fetch a Transaction created in a by the [MultiNodeDoubleChainBlockTestHelper]
 */
class TxCache(val chainMap: MutableMap<Int, ChainTxsCache> ) {

    companion object : KLogging()

    /* // REMOVE AFTER 2019-11-11
    fun getCachedTxRid(chainId: Int, nrOfChains: Int, height: Int, txPerBlock: Int, counter: Int): ByteArray {
        val baseIndex = height * txPerBlock + counter
        val index =  baseIndex * nrOfChains + chainId -1
        val expectedTxRid = internalTxList[index].getRID()
        logger.debug("Fetching cached TX with index: $index (chainId: $chainId, height: $height, TX pos: $counter):  TX RID: ${expectedTxRid.toHex()}")
        return expectedTxRid

    }
    */

    fun getCachedTxRid(chainId: Int, height: Int, counter: Int): ByteArray {
        val chain = chainMap[chainId]!!
        val block = chain.blockList[height]
        val expectedTxRid = block.txList[counter].getRID()
        logger.debug("Fetching cached TX  (chainId: $chainId, height: $height, TX pos: $counter):  TX RID: ${expectedTxRid.toHex()}")
        return expectedTxRid
    }

    /**
     * Adds a TX to the cache.
     */
    fun addTx(tx: GTXTransaction, chainId: Int, height: Int, counter: Int) {
        var chain = chainMap[chainId]
        if (chain == null) {
            logger.debug ("Must add new chain $chainId")
            chain = ChainTxsCache(chainId, mutableListOf())
            chainMap[chainId] = chain
        }

        chain!!.addTx(tx, chainId, height, counter)
    }
}

data class ChainTxsCache (
        val chainId: Int,
        val blockList: MutableList<BlockTxCache>
) {

    /**
     * Adding the [GTXTransaction] to the correct block in the chain
     */
    fun addTx(tx: GTXTransaction, chainId: Int, height: Int, counter: Int) {
        if (blockList.size <= height) {
            TxCache.logger.debug ("Must add new block to chain $chainId of height: $height")
            val newBlock = BlockTxCache(height, mutableListOf())
            blockList.add(newBlock)
        }

        blockList[height].addTx(tx, chainId, counter)
    }
}

data class BlockTxCache (
        val height: Int,
        val txList: MutableList<GTXTransaction>) {


    /**
     * Adding the [GTXTransaction] to the pos [counter] in the block.
     * Problem is that there might be a TX there already.
     */
    fun addTx(tx: GTXTransaction, chainId: Int, counter: Int) {
        if (txList.size > counter) {
            val foundTx = txList[counter]
            if (foundTx.getRID().contentEquals(tx.getRID())) {
                TxCache.logger.debug("We already added this TX at pos: $counter in block: $height, for chain: $chainId")
            } else {
                throw IllegalArgumentException("There was a different TX on pos $counter in block: $height, for chain: $chainId")
            }
        } else {
            txList.add(tx)
        }
    }

}
