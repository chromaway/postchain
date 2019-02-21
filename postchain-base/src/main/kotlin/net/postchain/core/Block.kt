package net.postchain.core

import net.postchain.common.toHex

interface BlockHeader {
    val prevBlockRID: ByteArray
    val rawData: ByteArray
    val blockRID: ByteArray // it's not a part of header but derived from it
}

open class BlockData(
        val header: BlockHeader,
        val transactions: List<ByteArray>)


/**
 * BlockDetail returns a more in deep block overview
 * ATM it is mainly used to reply to explorer's queries
 */
open class BlockDetail (
        val header: ByteArray,
        val height: Long,
        val transactions: List<ByteArray>,
        val witness: ByteArray,
        val timestamp: Long
) {
    fun equals(other: BlockDetail): Boolean {
        if (this.height != other.height) return false
        if (this.witness.toHex() == other.witness.toHex()) return false

        if (this.transactions.size != other.transactions.size) return false

        val thisTransactionsToHex = transactions.map{ transaction -> transaction.toHex()}
        for (transaction in  other.transactions) {
            if (!thisTransactionsToHex.contains(transaction.toHex())) return false
        }

        return true
    }
}

data class ValidationResult(
        val result: Boolean,
        val message: String? = null)

/**
 * Witness is a generalization over signatures.
 * Block-level witness is something which proves that block is valid and properly authorized.
 */
interface BlockWitness {
    //    val blockRID: ByteArray
    fun getRawData(): ByteArray
}

open class BlockDataWithWitness(header: BlockHeader, transactions: List<ByteArray>, val witness: BlockWitness?)
    : BlockData(header, transactions)

interface MultiSigBlockWitness : BlockWitness {
    fun getSignatures(): Array<Signature>
}

class InitialBlockData(
        val blockIID: Long,
        val chainID: Long,
        val prevBlockRID: ByteArray,
        val height: Long,
        val timestamp: Long)
