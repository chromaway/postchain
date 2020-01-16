package net.postchain.base

import net.postchain.core.BadDataMistake
import net.postchain.core.BadDataType
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvString

object BaseDependencyFactory {

    /**
     * Turns the BC dependency configuration into a number of [BlockchainRelatedInfo] instances.
     */
    fun build(dep: Gtv): List<BlockchainRelatedInfo>  {
        try {
            // Should contain an array of String, ByteArr pairs
            val gtvDepArray = dep!! as GtvArray
            val depList = mutableListOf<BlockchainRelatedInfo>()
            for (element in gtvDepArray.array) {
                val elemArr = element as GtvArray
                val nickname = elemArr[0] as GtvString
                val blockchainRid = elemArr[1] as GtvByteArray
                depList.add(
                        BlockchainRelatedInfo(blockchainRid.bytearray, nickname.string, null)
                )

            }
            return depList.toList()
        } catch (e: Exception) {
            throw BadDataMistake(BadDataType.BAD_CONFIGURATION,
                    "Dependencies must be array of array and have two parts, one string (description) and one bytea (blokchain RID)", e)
        }
    }
}