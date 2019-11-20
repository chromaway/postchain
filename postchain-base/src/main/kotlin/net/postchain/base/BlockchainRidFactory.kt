package net.postchain.base

import net.postchain.base.data.DatabaseAccess
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.core.EContext
import net.postchain.devtools.PeerNameHelper
import net.postchain.gtv.Gtv
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
import net.postchain.gtv.merkleHash

object BlockchainRidFactory {

    val cryptoSystem = SECP256K1CryptoSystem()
    val merkleHashCalculator = GtvMerkleHashCalculator(cryptoSystem)

    /**
     * Check if there is a Blockchain RID for this chain already
     * If no Blockchain RID, we use this config to create a BC RID, since this MUST be the first config, and store to DB.
     *
     * TODO: Olle Can we check in any other way if the GTV config we are holding is NOT the first config?
     *
     * @param data is the [Gtv] data of the configuration
     * @param merkleHashCalculator needed if the BC RID does not exist
     * @param eContext
     * @return the blockchain's RID, either old or just created
     */
    fun resolveBlockchainRID(
            data: Gtv,
            eContext: EContext
    ): BlockchainRid {
        val dbBcRid = DatabaseAccess.of(eContext).getBlockchainRID(eContext)
        if (dbBcRid != null) {
            return dbBcRid // We have it in the DB so don't do anything (if it's in here it must be correct)
        } else {
            // Need to calculate it the RID, and we do it the usual way (same as merkle root of block)
            val bcRid = data.merkleHash(merkleHashCalculator)
            DatabaseAccess.of(eContext).checkBlockchainRID(eContext, BlockchainRid(bcRid))
            return BlockchainRid(bcRid)
        }
    }
}

data class BlockchainRid(val data: ByteArray) {

    companion object {

        val EMPTY_RID = BlockchainRid(ByteArray(0))
        val FULL_EMPTY_RID = BlockchainRid(ByteArray(64, Int::toByte)) // This will actually have the same size as a real RID

        fun buildFromHex(str: String) = BlockchainRid(str.hexStringToByteArray())

        /**
         * For test, build a full length BC RID by repeating a single digit as a byte
         *
         * @param b is the byte to be repeated
         */
        fun buildRepeat(b: Byte): BlockchainRid {
            val bArr = ByteArray(64, {b})
            return BlockchainRid(bArr)
        }
    }

    fun toHex() = data.toHex()

    fun toShortHex() = PeerNameHelper.shortHash(data)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlockchainRid

        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }


}