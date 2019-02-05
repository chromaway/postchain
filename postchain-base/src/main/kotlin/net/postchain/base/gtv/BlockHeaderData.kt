package net.postchain.base.gtv

import net.postchain.gtv.*
import net.postchain.gtv.GtvFactory.gtv

/**
 * The structure of the block header goes like this:
 *
 *  1. bockchainRid [GtvByteArray]
 *  2. prevBlockRid [GtvByteArray]
 *  3. rootHash [GtvByteArray]
 *  4. timestamp [GtvInteger]
 *  5. height [GtvInteger]
 *  6. extra  [GtvDictionary]
 */
data class BlockHeaderData(
        val gtvBlockchainRid: GtvByteArray,
        val gtvPreviousBlockRid: GtvByteArray,
        val gtvMerkleRootHash: GtvByteArray,
        val gtvTimestamp: GtvInteger,
        val gtvHeight: GtvInteger,
        val gtvExtra: GtvDictionary) {


    fun getBlockchainRid(): ByteArray {
        return gtvBlockchainRid.bytearray
    }

    fun getPreviousBlockRid(): ByteArray {
        return gtvPreviousBlockRid.bytearray
    }

    fun getMerkleRootHash(): ByteArray {
        return gtvMerkleRootHash.bytearray
    }

    fun getTimestamp(): Long {
        return gtvTimestamp.integer
    }

    fun getHeight(): Long {
        return gtvHeight.integer
    }

    fun getExtra(): Map<String, String> {
        val retMap = HashMap<String, String>()
        for (key in this.gtvExtra.dict.keys) {
            val gtvValue = gtvExtra[key] as GtvString
            retMap.put(key, gtvValue.string)
        }
        return retMap
    }

    fun toGtv(): GtvArray {
        return gtv(gtvBlockchainRid, gtvPreviousBlockRid, gtvMerkleRootHash, gtvTimestamp, gtvHeight, gtvExtra)
    }

}