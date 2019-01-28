package net.postchain.base.merkle


/**
 * A collection of proof paths.
 */
interface MerklePathSet {

    /**
     * @return true if there are no paths left
     */
    fun isEmpty(): Boolean

    /**
     * @return true if one of the paths is a leaf
     */
    fun isThisAProofLeaf(): Boolean
}