package net.postchain.base.merkle.root

import net.postchain.base.merkle.MerklePathSet

class HashPathSet(paths: Set<ByteArray>): MerklePathSet {

    override fun isEmpty(): Boolean {
        return false
    }

    override fun isThisAProofLeaf(): Boolean {
        return true
    }
}