// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.merkle.proof

import net.postchain.base.merkle.BinaryTreeFactory
import net.postchain.common.data.Hash
import net.postchain.base.merkle.MerkleHashCalculator
import net.postchain.base.path.PathSet
import net.postchain.gtv.Gtv


/**
 * This factory will take you directly from a source structure to the merkle root hash.
 * It does not handle proofs
 *
 * Note: When calculating the merkle root of a proof of a complicated structure (args or dict)
 *       means that the value-to-be-proved (i.e. args/dict) must be transformed to a binary tree
 *       before we can calculate it's hash.
 *       (This is why we place this code in a separate factory instead of in the [MerkleProofTree] itself)
 */
abstract class MerkleHashSummaryFactory<T, TPathSet: PathSet>(
        val treeFactory: BinaryTreeFactory<T, TPathSet>,
        val proofFactory: MerkleProofTreeFactory<T>) {

    /**
     * Calculates all the way from source type to merkle hash.
     *
     * Note: should have looked in cache before this, because here we will do the calculation no matter what.
     *
     * @param value is the source
     * @param calculator holds the function we'll use to hash & serialize
     * @return the calculated merkle root of the proof.
     */
    abstract fun calculateMerkleRoot(value: T, calculator: MerkleHashCalculator<T>): MerkleHashSummary

    /**
     * Calculates the merkle root of the given proof tree.
     *
     * (If the original [Gtv] structure was a block, for the proof to be valid, the returned [Hash] should equal
     * the merkle root of the block.)
     *
     * @param proofTree the tree we are going to calculate the merkle root of (in the cases where there is no path, this
     *                  has already been done, and we just return the top element)
     * @param calculator holds the function we'll use to hash & serialize
     * @param treeFactory is needed sometimes (see above)
     * @return the calculated merkle root of the proof.
     */
    fun calculateMerkleRoot(proofTree: MerkleProofTree<T>, calculator: MerkleHashCalculator<T>): MerkleHashSummary {
        val calculatedSummary = calculateMerkleRootInternal(proofTree.root, calculator)
        return MerkleHashSummary(calculatedSummary, proofTree.totalNrOfBytes)
    }

    @Suppress("UNCHECKED_CAST")
    private fun calculateMerkleRootInternal(currentElement: MerkleProofElement, calculator: MerkleHashCalculator<T>): Hash {
        return when (currentElement) {
            is ProofHashedLeaf -> currentElement.merkleHash
            is ProofValueLeaf<*> -> {
                val value = currentElement.content as T // Compiler "unchecked cast" warning here, but this is actually safe.
                if (calculator.isContainerProofValueLeaf(value)) {
                    // We have a container value to prove, so need to convert the value to a binary tree, and THEN hash it
                    val merkleProofTree: MerkleProofTree<T> = buildProofTree(value, calculator)
                    calculateMerkleRootInternal(merkleProofTree.root, calculator)
                } else {
                    calculator.calculateLeafHash(value)
                }
            }
            is ProofNode -> {
                val left = calculateMerkleRootInternal(currentElement.left, calculator)
                val right = calculateMerkleRootInternal(currentElement.right, calculator)
                calculator.calculateNodeHash(currentElement.prefix ,left, right)
            }
            else -> {
                throw IllegalStateException("Should have handled this type: $currentElement")
            }
        }
    }

    /**
     * @param value is the structure we want to turn to a proof tree
     * @param treeFactory needed to build the proof tree
     * @return the new proof tree
     */
    abstract fun buildProofTree(value: T, calculator: MerkleHashCalculator<T>): MerkleProofTree<T>
}