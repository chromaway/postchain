package net.postchain.base.gtv

import net.postchain.gtv.*
import net.postchain.gtv.GtvFactory.gtv

/**
 * The structure of the block header goes like this:
 *
 *  1. prevBlockRid [GtvByteArray]
 *  2. rootHash [GtvByteArray]
 *  3. timestamp [GtvInteger]
 *  4. height [GtvInteger]
 *  5. extra  [GtvDictionary]
 */
data class BlockHeaderData(
        val gtvPreviousBlockRid: GtvByteArray,
        val gtvMerkleRootHash: GtvByteArray,
        val gtvTimestamp: GtvInteger,
        val gtvHeight: GtvInteger,
        val gtvExtra: GtvDictionary) {

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
        return gtv(gtvPreviousBlockRid, gtvMerkleRootHash, gtvTimestamp, gtvHeight, gtvExtra)
    }

}