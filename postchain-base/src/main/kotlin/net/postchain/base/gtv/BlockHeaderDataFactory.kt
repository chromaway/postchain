package net.postchain.base.gtv

import net.postchain.core.InitialBlockData
import net.postchain.gtv.*
import net.postchain.gtv.GtvFactory.gtv
import kotlin.concurrent.timer

object BlockHeaderDataFactory {

    fun buildFromBinary(rawData: ByteArray): BlockHeaderData {
        val gtv: Gtv = GtvFactory.decodeGtv(rawData)
        return buildFromGtv(gtv)
    }

    fun buildFromGtv(gtvBlockHeader: Gtv): BlockHeaderData {
        val gtvMainArr = gtvBlockHeader as GtvArray

        // Fill it up some descriptive variables (not really needed but...)
        val previousBlockRid: GtvByteArray = gtvMainArr[0] as GtvByteArray
        val merkleRootHash: GtvByteArray = gtvMainArr[1] as GtvByteArray
        val timestamp: GtvInteger = gtvMainArr[2] as GtvInteger
        val height: GtvInteger = gtvMainArr[3] as GtvInteger
        val extra: GtvDictionary = gtvMainArr[4] as GtvDictionary

        return BlockHeaderData(previousBlockRid, merkleRootHash, timestamp, height, extra)
    }

    fun buildFromDomainObjects(iBlockData: InitialBlockData, rootHash: ByteArray, timestamp: Long): BlockHeaderData {
        val previousBlockRid: GtvByteArray = gtv(iBlockData.prevBlockRID)
        val merkleRootHash: GtvByteArray = gtv(rootHash)
        val timestamp: GtvInteger = gtv(timestamp)
        val height: GtvInteger = gtv(iBlockData.height)
        val extra = GtvDictionary(HashMap<String, Gtv>())

        return BlockHeaderData(previousBlockRid, merkleRootHash, timestamp, height, extra)
    }
}