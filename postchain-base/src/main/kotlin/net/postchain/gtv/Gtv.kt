package net.postchain.gtv

import net.postchain.base.merkle.Hash
import net.postchain.base.merkle.MerkleHashCalculator
import net.postchain.base.merkle.proof.MerkleHashSummary
import net.postchain.gtv.merkle.GtvMerkleBasics
import net.postchain.gtv.merkle.proof.GtvMerkleProofTree
import net.postchain.gtv.messages.Gtv as RawGtv

/**
 * Enum class of GTXML types
 * Note: order is same as in ASN.1, thus we can use same integer ids.
 */
enum class GtvType {
    NULL, BYTEARRAY, STRING, INTEGER, DICT, ARRAY
}

/**
 * GTV stands for Generic Transfer Value, and is a (home made) format for data transfer.
 */
interface Gtv {
    val type: GtvType

    // Collection methods here
    operator fun get(index: Int): Gtv
    operator fun get(key: String): Gtv?

    // Convert to sub-class
    fun asString(): String
    fun asArray(): Array<out Gtv>
    fun isNull(): Boolean
    fun asDict(): Map<String, Gtv>
    fun asInteger(): Long
    fun asByteArray(convert: Boolean = false): ByteArray

    // Other conversions
    fun asPrimitive(): Any?
    fun getRawGtv(): RawGtv

    fun nrOfBytes(): Int

    // Caching
    fun getCachedMerkleHash(): MerkleHashSummary?
    fun setCachedMerkleHash(summary: MerkleHashSummary)
}


/**
 * Calculates the merkle root hash of the structure.
 *
 * Note: if the hash is present in the cache, the cached value will be returned instead.
 *
 * @param calculator describes the method we use for hashing and serialization
 * @return the merkle root hash (32 bytes) of the [Gtv] structure.
 */
fun Gtv.merkleHash(calculator: MerkleHashCalculator<Gtv>): Hash {
    return merkleHashSummary(calculator).merkleHash
}

/**
 * Calculates the merkle root hash of the structure OR fetches it from the cache.
 *
 * 1. We will begin by looking in the [Gtv] 's local cache
 * 2. If not found, we look in the global cache
 * 3. If not found, we need to calculate it
 * 4. Remember to update both caches if they were empty
 *
 * @param calculator describes the method we use for hashing and serialization
 * @return the merkle root hash summary
 */
fun Gtv.merkleHashSummary(calculator: MerkleHashCalculator<Gtv>): MerkleHashSummary {
    var foundInCache = false
    val cachedSummary = calculator.memoization.findMerkleHash(this)

    val newSummary = if (cachedSummary != null) {
        foundInCache = true
        cachedSummary
    } else {
        // Need to calculate hash
        val summaryFactory = GtvMerkleBasics.getGtvMerkleHashSummaryFactory()
        summaryFactory.calculateMerkleRoot(this, calculator)
    }

    // Update cache
    if (!foundInCache) {
        calculator.memoization.add(this, newSummary)
    }

    return newSummary
}

/**
 * Creates a proof out of the given path described as indexs of an array
 *
 * @param array indexes that points to the element we need to prove
 * @param calculator describes the method we use for hashing and serialization
 * @return the created proof tree
 */
fun Gtv.generateProof(indexOfElementsToProve: List<Int>, calculator: MerkleHashCalculator<Gtv>): GtvMerkleProofTree {

    val gtvPathList: List<GtvPath> = indexOfElementsToProve.map { GtvPathFactory.buildFromArrayOfPointers(arrayOf(it)) }
    val gtvPaths = GtvPathSet(gtvPathList.toSet())
    return this.generateProof(gtvPaths, calculator)
}

/**
 * Creates a proof out of the given [GtvPathSet]
 *
 * @param gtvPaths the paths to the elements we need to prove
 * @param calculator describes the method we use for hashing and serialization
 * @return the created proof tree
 */
fun Gtv.generateProof(gtvPaths: GtvPathSet, calculator: MerkleHashCalculator<Gtv>): GtvMerkleProofTree {
    val factory = GtvMerkleBasics.getGtvBinaryTreeFactory()
    val proofFactory = GtvMerkleBasics.getGtvMerkleProofTreeFactory()

    val binaryTree = factory.buildFromGtvAndPath(this ,gtvPaths, calculator.memoization)

    return proofFactory.buildFromBinaryTree(binaryTree, calculator)
}

