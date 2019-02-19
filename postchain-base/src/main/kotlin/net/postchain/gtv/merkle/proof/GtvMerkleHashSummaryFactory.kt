package net.postchain.gtv.merkle.proof

import net.postchain.base.merkle.MerkleHashCalculator
import net.postchain.base.merkle.proof.MerkleHashSummary
import net.postchain.base.merkle.proof.MerkleHashSummaryFactory
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvPathSet
import net.postchain.gtv.merkle.GtvBinaryTree
import net.postchain.gtv.merkle.GtvBinaryTreeFactory

class GtvMerkleHashSummaryFactory(
        treeFactory: GtvBinaryTreeFactory,
        proofFactory: GtvMerkleProofTreeFactory
): MerkleHashSummaryFactory<Gtv, GtvPathSet>(treeFactory, proofFactory) {

    override fun calculateMerkleRoot(value: Gtv, calculator: MerkleHashCalculator<Gtv>): MerkleHashSummary {
        val gtvTreeFactory = treeFactory as GtvBinaryTreeFactory
        val binaryTree = gtvTreeFactory.buildFromGtv(value, calculator.memoization)

        val gtvProofFactory = proofFactory as GtvMerkleProofTreeFactory
        val proofTree = gtvProofFactory.buildFromBinaryTree(binaryTree, calculator)

        return calculateMerkleRoot(proofTree, calculator)
    }

    override fun buildProofTree(value: Gtv, calculator: MerkleHashCalculator<Gtv>): GtvMerkleProofTree {
        val gtvTreeFactory = treeFactory as GtvBinaryTreeFactory
        val root: GtvBinaryTree = gtvTreeFactory.buildFromGtv(value, calculator.memoization)
        val gtvProofFactory = proofFactory as GtvMerkleProofTreeFactory
        return gtvProofFactory.buildFromBinaryTree(root, calculator)
    }
}