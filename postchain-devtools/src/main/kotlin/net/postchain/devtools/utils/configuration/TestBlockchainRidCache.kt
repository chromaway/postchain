package net.postchain.devtools.utils.configuration

import net.postchain.base.BlockchainRid
import net.postchain.base.BlockchainRidFactory
import net.postchain.gtv.Gtv

/**
 * This cache will save us some CPU and get us out of situations where we really should have gone to the DB to fetch the RID / IID.
 *
 * TODO: Olle Since CHAIN -> RID never changes (we can only add more) this cache could actually be used in production code to to avoid DB calls.
 */
object TestBlockchainRidCache {

    val cacheRid = mutableMapOf<Int, BlockchainRid>()
    val cacheChainId = mutableMapOf<BlockchainRid, Int>()

    fun clear() {
        cacheRid.clear()
        cacheChainId.clear()
    }

    fun add(chainIid: Int, bcRid: BlockchainRid) {
        cacheChainId[bcRid] = chainIid
        cacheRid[chainIid] = bcRid
    }

    fun getRid(chainId: Int, bcGtv: Gtv): BlockchainRid = cacheRid[chainId] ?: calcAndAdd(chainId, bcGtv)

    fun getChainId(rid: BlockchainRid): Int = cacheChainId[rid]
            ?: throw IllegalStateException("Is ${rid.toShortHex()} a dependency bc RID? This chain must be added to the cache before it can be found. (${this.toString()}")

    fun calcAndAdd(chainId: Int, bcGtv: Gtv): BlockchainRid {
        val newRid = BlockchainRidFactory.calculateBlockchainRid(bcGtv)
        add(chainId, newRid)
        return newRid
    }

    override fun toString(): String {
        val sb = StringBuffer()
        for (chainId in cacheRid.keys) {
            sb.append("[$chainId]= ${cacheRid[chainId]!!.toShortHex()}, ")
        }
        return sb.toString()
    }

}
