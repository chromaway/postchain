package net.postchain.base.merkle


/**
 * This construct can be used to validate blocks
 */
class HashFullBinaryTree(root: FbtElement) : ContentLeafFullBinaryTree<Hash>(root) {
}


class HashFullBinaryTreeFactory : CompleteBinaryTreeFactory<Hash>() {

    fun buildFromArrayList(originalList: List<Hash>): HashFullBinaryTree {
        val result = buildSubTree(originalList)
        return HashFullBinaryTree(result)
    }

    /**
     * Needed to override this, to handle [Hash] leafs
     */
    override fun handleLeaf(leaf: Hash): FbtElement {
        return Leaf(leaf)
    }
}