package net.postchain.base.merkle.root

import net.postchain.base.merkle.*


// TODO: Proofs here too!
/**
 * This construct can be used to validate entire blocks (if we have the hashes of all transactions).
 *
 * NOTE: This tree structure is not strictly needed, since the more complex [GtxBinaryTree] can be used for
 *       trees of only hashes too, but this is simpler to understand so it's probably motivated anyway.
 */
class HashBinaryTree(root: BlockRootNode) : BinaryTree<Hash>(root) {
}

/**
 * Represents the root of an entire block
 */
class BlockRootNode(left: BinaryTreeElement, right: BinaryTreeElement): Node(left, right) {

    companion object BlockRootNodeCompanion{
        const val prefixByte: Byte = 160.toByte()  // A0 in hex
    }

    override fun getPrefixByte(): Byte {
        return prefixByte
    }
}

