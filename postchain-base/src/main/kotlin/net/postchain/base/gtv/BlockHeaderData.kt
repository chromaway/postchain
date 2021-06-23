// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.gtv

import net.postchain.common.data.Hash
import net.postchain.core.BadDataMistake
import net.postchain.core.BadDataType
import net.postchain.core.UserMistake
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
 *  6. dependencies [GtvArray] or [GtvNull] (this is to save space)
 *  6. extra  [GtvDictionary]
 */
data class BlockHeaderData(
        val gtvBlockchainRid: GtvByteArray,
        val gtvPreviousBlockRid: GtvByteArray,
        val gtvMerkleRootHash: GtvByteArray,
        val gtvTimestamp: GtvInteger,
        val gtvHeight: GtvInteger,
        val gtvDependencies: Gtv, // Can be either GtvNull or GtvArray
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
        return gtvTimestamp.integer.toLong()
    }

    fun getHeight(): Long {
        return gtvHeight.integer.toLong()
    }

    /**
     * Turns the [gtvDependencies] into an array of [Hash].
     *
     * Note that empty BC dependencies are allowed. This means that the BC we depend on has no blocks.
     * (We allow this bc it's easier to get started, specially during test)
     */
    fun getBlockHeightDependencyArray(): Array<Hash?> {
        return when (gtvDependencies) {
            is GtvNull -> arrayOf()
            is GtvArray -> {
                val lastBlockRidArray = arrayOfNulls<Hash>(gtvDependencies.getSize())
                var i = 0
                for (blockRid in gtvDependencies.array) {
                    lastBlockRidArray[i] = when (blockRid) {
                        is GtvByteArray -> blockRid.bytearray
                        is GtvNull -> null // Allowed
                        else -> throw UserMistake("Cannot use type ${blockRid.type} in dependency list (at pos: $i)")
                    }
                    i++
                }
                lastBlockRidArray
            }
            else -> throw BadDataMistake(BadDataType.BAD_BLOCK,
                    "Header data has incorrect format in dependency part, where we found type: ${gtvDependencies.type}")
        }

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
        return gtv(gtvBlockchainRid, gtvPreviousBlockRid, gtvMerkleRootHash, gtvTimestamp, gtvHeight, gtvDependencies, gtvExtra)
    }

    companion object {

        fun fromGtv(gtv: GtvArray): BlockHeaderData {
            return BlockHeaderData(
                    gtv[0] as GtvByteArray,
                    gtv[1] as GtvByteArray,
                    gtv[2] as GtvByteArray,
                    gtv[3] as GtvInteger,
                    gtv[4] as GtvInteger,
                    gtv[5],
                    gtv[6] as GtvDictionary)
        }
    }

}