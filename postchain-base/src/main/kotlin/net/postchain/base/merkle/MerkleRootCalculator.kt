package net.postchain.base.merkle

import net.postchain.gtv.*
import net.postchain.gtv.merkle.GtvBinaryTreeFactory
import net.postchain.gtv.merkle.proof.GtvMerkleProofTree
import net.postchain.gtv.merkle.proof.GtvMerkleProofTreeFactory
import net.postchain.gtv.merkle.proof.ProofNodeGtvArrayHead

/**
 * Responsible for calculating the merkle root from a list of transactions hashes.
 *
 *
 */
class MerkleRootCalculator(val calculator: MerkleHashCalculator<Gtv>) {

    val factory = GtvBinaryTreeFactory()
    val proofFactory = GtvMerkleProofTreeFactory(calculator)

    fun calculateMerkleRoot(txHashes: List<Hash>): Hash {
        // 1. Convert to GtvArray
        val gtvArr = GtvFactory.gtv(txHashes.map { GtvFactory.gtv(it) })

        // 2. Build Binary tree out of GtvArray
        val binaryTree = factory.buildFromGtv(gtvArr)

        // 3. Build ProofTree
        val proofTree = proofFactory.buildFromBinaryTree(binaryTree)

        // 4. Pick the root elements root
        return proofTree.calculateMerkleRoot(this.calculator)
    }

    fun generateProof(txHashes: List<Hash>, indexOfHashesToProve: List<Int>): GtvMerkleProofTree {
        // 1. Convert to GtvArray
        val gtvArr = GtvFactory.gtv(txHashes.map { GtvFactory.gtv(it) })

        // 2. Transform simple number into path
        val gtvPathList: List<GtvPath> = indexOfHashesToProve.map { GtvPathFactory.buildFromArrayOfPointers(arrayOf(it)) }
        val gtvPaths = GtvPathSet(gtvPathList.toSet())

        // 3. Build Binary tree out of GtvArray
        val binaryTree = factory.buildFromGtvAndPath(gtvArr,gtvPaths)

        // 4. return the ProofTree
        return proofFactory.buildFromBinaryTree(binaryTree)
    }
}