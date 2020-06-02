// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

import net.postchain.base.data.DatabaseAccess
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.core.EContext
import net.postchain.gtv.Gtv
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
import net.postchain.gtv.merkleHash

object BlockchainRidFactory {

    val cryptoSystem = SECP256K1CryptoSystem()
    private val merkleHashCalculator = GtvMerkleHashCalculator(cryptoSystem)

    /**
     * Check if there is a Blockchain RID for this chain already
     * If no Blockchain RID, we use this config to create a BC RID, since this MUST be the first config, and store to DB.
     *
     * TODO: Olle Can we check in any other way if the GTV config we are holding is NOT the first config?
     * TODO: [POS-128]: Eliminate this method
     *
     * @param data is the [Gtv] data of the configuration
     * @param merkleHashCalculator needed if the BC RID does not exist
     * @param eContext
     * @return the blockchain's RID, either old or just created
     */
    @Deprecated("TODO: [POS-128]")
    fun resolveBlockchainRID(
            data: Gtv,
            eContext: EContext
    ): BlockchainRid {
        val dbBcRid = DatabaseAccess.of(eContext).getBlockchainRid(eContext)
        return if (dbBcRid != null) {
            dbBcRid // We have it in the DB so don't do anything (if it's in here it must be correct)
        } else {
            val newRid = calculateBlockchainRid(data)
            DatabaseAccess.of(eContext).checkBlockchainRID(eContext, newRid)
            newRid
        }
    }

    /**
     * Calculates blockchain RID by the given blockchain configuration.
     *
     * @param data is the [Gtv] data of the configuration
     * @return the blockchain RID
     */
    fun calculateBlockchainRid(data: Gtv): BlockchainRid {
        // Need to calculate it the RID, and we do it the usual way (same as merkle root of block)
        val bcBinary = data.merkleHash(merkleHashCalculator)
        return BlockchainRid(bcBinary)
    }
}

data class BlockchainRid(val data: ByteArray) {

    /*
    // TODO Olle Add this and fix test-
    init {
        if (data.size != 32) {
            throw IllegalArgumentException("Wrong size of Blockchain RID, was ${data.size} should be 32")
        }
    }
    */

    companion object {

        val EMPTY_RID = BlockchainRid(ByteArray(0))

        fun buildFromHex(str: String) = BlockchainRid(str.hexStringToByteArray())

        /**
         * For test, build a full length BC RID by repeating a single digit as a byte
         *
         * @param b is the byte to be repeated
         */
        fun buildRepeat(b: Byte): BlockchainRid {
            val bArr = ByteArray(32) { b }
            return BlockchainRid(bArr)
        }
    }

    fun toHex() = data.toHex()

    fun toShortHex(): String {
        return toHex().run {
            "${take(2)}:${takeLast(2)}"
        }
    }

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

    override fun toString(): String {
        return toHex()
    }
}