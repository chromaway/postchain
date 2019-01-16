package net.postchain.base.merkle.root

import net.postchain.base.merkle.*


class HashBinaryTreeFactory : BinaryTreeFactory<Hash, ByteArray>() {

    /**
     * Will take a list of hashes and generate a binary tree from it.
     *
     * We have the corner cases:
     * 1. of just one hash, and for this we need to add a dummy right element
     * 2. empty list -> root node with 2 empty leafs
     */
    fun buildFromList(originalList: List<Hash>): HashBinaryTree {

        val result = buildSubTreeFromLeafList(originalList)
        val newRoot = when (result) {
            is Node -> BlockRootNode(result.left, result.right)
            is Leaf<*> -> BlockRootNode(result, EmptyLeaf)
            is EmptyLeaf -> BlockRootNode(EmptyLeaf, EmptyLeaf)
            else -> {
                throw IllegalStateException("What did we get back? " + result.toString())
            }
        }
        return HashBinaryTree(newRoot)
    }

    /**
     * Needed to override this.
     * @param leaf the hash that should be converted
     * @param pathList wont be used (a bit ugly that we need to send it)
     * @return A [Leaf] with the hash in it
     */
    override fun handleLeaf(leaf: Hash, pathList: List<ByteArray>): BinaryTreeElement {
        return Leaf(leaf)
    }
}