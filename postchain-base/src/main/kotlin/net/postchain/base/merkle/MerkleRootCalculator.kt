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


}