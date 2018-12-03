package net.postchain.core

interface BlockHeader {
    val prevBlockRID: ByteArray
    val rawData: ByteArray
    val blockRID: ByteArray // it's not a part of header but derived from it
}

open class BlockData(
        val header: BlockHeader,
        val transactions: List<ByteArray>)

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
